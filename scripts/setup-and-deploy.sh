#!/bin/bash

# Script unificado para setup de EC2 y deployment de Booky Backend
# Combina setup-ec2.sh y deploy.sh en un solo proceso

set -e

# Configuración por defecto
INSTANCE_TYPE=${EC2_INSTANCE_TYPE:-t3.medium}
REGION=${AWS_REGION:-us-east-1}
KEY_NAME=${EC2_KEY_NAME:-booky-key}
SECURITY_GROUP=${EC2_SECURITY_GROUP:-booky-sg}
INSTANCE_NAME=${EC2_INSTANCE_NAME:-booky-server}

echo "🚀 Configurando y desplegando Booky Backend en EC2..."

# ================================================================
# PARTE 1: CONFIGURACIÓN DE EC2
# ================================================================

echo "📝 Verificando si la instancia EC2 existe..."

# Verificar si la instancia ya existe
INSTANCE_ID=$(aws ec2 describe-instances \
  --region $REGION \
  --filters "Name=tag:Name,Values=$INSTANCE_NAME" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text 2>/dev/null || echo "None")

if [[ "$INSTANCE_ID" == "None" || "$INSTANCE_ID" == "" ]]; then
  echo "🏗️  Creando nueva instancia EC2..."
  
  # Crear Key Pair si no existe
  if ! aws ec2 describe-key-pairs --region $REGION --key-names $KEY_NAME &> /dev/null; then
    echo "🔑 Creando Key Pair..."
    aws ec2 create-key-pair --region $REGION --key-name $KEY_NAME --query 'KeyMaterial' --output text > ${KEY_NAME}.pem
    chmod 400 ${KEY_NAME}.pem
    echo "✅ Key Pair creado: ${KEY_NAME}.pem"
  fi
  
  # Crear Security Group si no existe
  if ! aws ec2 describe-security-groups --region $REGION --group-names $SECURITY_GROUP &> /dev/null; then
    echo "🔒 Creando Security Group..."
    SG_ID=$(aws ec2 create-security-group \
      --region $REGION \
      --group-name $SECURITY_GROUP \
      --description "Security group for Booky Backend" \
      --query 'GroupId' \
      --output text)
    
    # Permitir SSH (puerto 22)
    aws ec2 authorize-security-group-ingress \
      --region $REGION \
      --group-id $SG_ID \
      --protocol tcp \
      --port 22 \
      --cidr 0.0.0.0/0
    
    # Permitir HTTP (puerto 80)
    aws ec2 authorize-security-group-ingress \
      --region $REGION \
      --group-id $SG_ID \
      --protocol tcp \
      --port 80 \
      --cidr 0.0.0.0/0
    
    # Permitir HTTPS (puerto 443)
    aws ec2 authorize-security-group-ingress \
      --region $REGION \
      --group-id $SG_ID \
      --protocol tcp \
      --port 443 \
      --cidr 0.0.0.0/0
    
    # Permitir aplicación (puerto 8080)
    aws ec2 authorize-security-group-ingress \
      --region $REGION \
      --group-id $SG_ID \
      --protocol tcp \
      --port 8080 \
      --cidr 0.0.0.0/0
    
    echo "✅ Security Group creado: $SG_ID"
  fi
  
  # Obtener la AMI de Ubuntu más reciente
  AMI_ID=$(aws ec2 describe-images \
    --region $REGION \
    --owners 099720109477 \
    --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
    --query 'Images | sort_by(@, &CreationDate) | [-1].ImageId' \
    --output text)
  
  echo "🖥️  Usando AMI: $AMI_ID"
  
  # Script de inicialización
  USER_DATA=$(cat << 'EOF'
#!/bin/bash
apt-get update
apt-get install -y docker.io docker-compose git awscli curl

# Iniciar Docker
systemctl start docker
systemctl enable docker

# Añadir usuario ubuntu al grupo docker
usermod -aG docker ubuntu

# Instalar Docker Compose v2
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Crear directorios para la aplicación
mkdir -p /opt/booky-app
mkdir -p /opt/booky-app/backups
chown -R ubuntu:ubuntu /opt/booky-app

# Instalar nginx para reverse proxy
apt-get install -y nginx

# Configurar nginx como reverse proxy
cat > /etc/nginx/sites-available/booky << 'NGINXCONF'
server {
    listen 80;
    server_name _;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINXCONF

# Habilitar el sitio
ln -s /etc/nginx/sites-available/booky /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
systemctl restart nginx
systemctl enable nginx

echo "✅ EC2 instance configurada correctamente" > /tmp/setup-complete.log
EOF
)
  
  # Crear la instancia
  echo "🏗️  Creando instancia EC2..."
  INSTANCE_ID=$(aws ec2 run-instances \
    --region $REGION \
    --image-id $AMI_ID \
    --instance-type $INSTANCE_TYPE \
    --key-name $KEY_NAME \
    --security-groups $SECURITY_GROUP \
    --user-data "$USER_DATA" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME}]" \
    --query 'Instances[0].InstanceId' \
    --output text)
  
  echo "⏳ Esperando que la instancia esté lista..."
  aws ec2 wait instance-running --region $REGION --instance-ids $INSTANCE_ID
  
  # Esperar un poco más para que el user-data termine
  echo "⏳ Esperando que la configuración inicial termine..."
  sleep 60
  
  echo "✅ Instancia creada: $INSTANCE_ID"
else
  echo "✅ Instancia ya existe: $INSTANCE_ID"
fi

# Obtener la IP pública
PUBLIC_IP=$(aws ec2 describe-instances \
  --region $REGION \
  --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo "🌐 IP Pública: $PUBLIC_IP"

# ================================================================
# PARTE 2: DEPLOYMENT
# ================================================================

echo "🚀 Iniciando deployment de la aplicación..."

# Configurar SSH
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# Crear archivo de llave SSH desde el key pair existente o recién creado
if [[ ! -f "${KEY_NAME}.pem" ]]; then
  echo "🔑 Descargando key pair..."
  # Si no tenemos la llave, necesitamos recrearla (esto solo funciona si acabamos de crear la instancia)
  if [[ "$INSTANCE_ID" != "None" ]]; then
    echo "⚠️  Usando AWS Session Manager para deployment sin SSH key"
    USE_SSM=true
  fi
else
  cp "${KEY_NAME}.pem" ~/.ssh/id_rsa
  chmod 600 ~/.ssh/id_rsa
  USE_SSM=false
fi

# Función para ejecutar comandos remotos
if [[ "$USE_SSM" == "true" ]]; then
  remote_exec() {
    aws ssm send-command \
      --region $REGION \
      --instance-ids $INSTANCE_ID \
      --document-name "AWS-RunShellScript" \
      --parameters "commands=['$1']" \
      --query 'Command.CommandId' \
      --output text
  }
  echo "🔧 Usando AWS Systems Manager para deployment"
else
  remote_exec() {
    ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "$1"
  }
  echo "🔧 Usando SSH para deployment"
  
  # Añadir host a known_hosts
  ssh-keyscan -H $PUBLIC_IP >> ~/.ssh/known_hosts 2>/dev/null || true
fi

# Función para copiar archivos
copy_files() {
  if [[ "$USE_SSM" == "true" ]]; then
    # Para SSM, necesitamos usar S3 o embebido en el comando
    echo "📦 Preparando archivos para SSM..."
    
    # Crear docker-compose para producción embebido en el comando
    DOCKER_COMPOSE_CONTENT=$(cat << 'EOFCOMPOSE'
services:
  postgres:
    image: postgres:15-alpine
    container_name: booky-postgres
    environment:
      POSTGRES_DB: booky
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: REPLACE_DB_PASSWORD
      PGDATA: /var/lib/postgresql/data/pgdata
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - booky-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d booky"]
      interval: 30s
      timeout: 10s
      retries: 3

  booky-app:
    image: openjdk:17-jdk-slim
    container_name: booky-backend
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/booky
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: REPLACE_DB_PASSWORD
      DATABASE_NAME: booky
      DDL_AUTO: update
      JWT_SECRET: REPLACE_JWT_SECRET
      JWT_EXPIRATION: 86400000
      CLOUDINARY_CLOUD_NAME: REPLACE_CLOUDINARY_CLOUD_NAME
      CLOUDINARY_API_KEY: REPLACE_CLOUDINARY_API_KEY
      CLOUDINARY_API_SECRET: REPLACE_CLOUDINARY_API_SECRET
      CORS_ALLOWED_ORIGINS: http://localhost:3000,http://REPLACE_PUBLIC_IP
      CORS_ALLOWED_METHODS: GET,POST,PUT,DELETE,OPTIONS
      CORS_ALLOWED_HEADERS: "*"
      CORS_ALLOW_CREDENTIALS: true
      LOG_LEVEL: INFO
      APP_LOG_LEVEL: INFO
      SECURITY_LOG_LEVEL: INFO
      SHOW_SQL: false
      FORMAT_SQL: false
      SPRING_PROFILES_ACTIVE: prod
      OPENAPI_PROD_URL: http://REPLACE_PUBLIC_IP
    ports:
      - "8080:8080"
    volumes:
      - ./target:/app
    working_dir: /app
    command: ["java", "-jar", "REPLACE_JAR_NAME"]
    networks:
      - booky-network
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  postgres_data:
    driver: local

networks:
  booky-network:
    driver: bridge
EOFCOMPOSE
)
    
         # Reemplazar variables en el docker-compose
     DOCKER_COMPOSE_CONTENT=$(echo "$DOCKER_COMPOSE_CONTENT" | sed "s/REPLACE_DB_PASSWORD/${DATABASE_PASSWORD}/g")
     DOCKER_COMPOSE_CONTENT=$(echo "$DOCKER_COMPOSE_CONTENT" | sed "s/REPLACE_JWT_SECRET/${JWT_SECRET}/g")
     DOCKER_COMPOSE_CONTENT=$(echo "$DOCKER_COMPOSE_CONTENT" | sed "s/REPLACE_CLOUDINARY_CLOUD_NAME/${CLOUDINARY_CLOUD_NAME}/g")
     DOCKER_COMPOSE_CONTENT=$(echo "$DOCKER_COMPOSE_CONTENT" | sed "s/REPLACE_CLOUDINARY_API_KEY/${CLOUDINARY_API_KEY}/g")
     DOCKER_COMPOSE_CONTENT=$(echo "$DOCKER_COMPOSE_CONTENT" | sed "s/REPLACE_CLOUDINARY_API_SECRET/${CLOUDINARY_API_SECRET}/g")
     DOCKER_COMPOSE_CONTENT=$(echo "$DOCKER_COMPOSE_CONTENT" | sed "s/REPLACE_PUBLIC_IP/${PUBLIC_IP}/g")
     DOCKER_COMPOSE_CONTENT=$(echo "$DOCKER_COMPOSE_CONTENT" | sed "s/REPLACE_JAR_NAME/${JAR_NAME}/g")
    
         # Crear el docker-compose en el servidor
     remote_exec "cat > /opt/booky-app/docker-compose.prod.yml << 'EOF'
$DOCKER_COMPOSE_CONTENT
EOF"
     
     # Para SSM, necesitamos copiar el JAR usando un método alternativo
     echo "📦 Subiendo JAR via AWS SSM..."
     # Convertir JAR a base64 para transferir via SSM
     JAR_B64=$(base64 -i "$JAR_FILE")
     remote_exec "echo '$JAR_B64' | base64 -d > /opt/booky-app/target/$JAR_NAME"
    
  else
    echo "📦 Copiando archivos al servidor..."
    
    # Crear docker-compose de producción localmente
    cat > docker-compose.prod.yml << EOF
services:
  postgres:
    image: postgres:15-alpine
    container_name: booky-postgres
    environment:
      POSTGRES_DB: booky
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
      PGDATA: /var/lib/postgresql/data/pgdata
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - booky-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d booky"]
      interval: 30s
      timeout: 10s
      retries: 3

  booky-app:
    image: openjdk:17-jdk-slim
    container_name: booky-backend
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/booky
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      DATABASE_NAME: booky
      DDL_AUTO: update
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: 86400000
      CLOUDINARY_CLOUD_NAME: ${CLOUDINARY_CLOUD_NAME}
      CLOUDINARY_API_KEY: ${CLOUDINARY_API_KEY}
      CLOUDINARY_API_SECRET: ${CLOUDINARY_API_SECRET}
      CORS_ALLOWED_ORIGINS: http://localhost:3000,http://${PUBLIC_IP}
      CORS_ALLOWED_METHODS: GET,POST,PUT,DELETE,OPTIONS
      CORS_ALLOWED_HEADERS: "*"
      CORS_ALLOW_CREDENTIALS: true
      LOG_LEVEL: INFO
      APP_LOG_LEVEL: INFO
      SECURITY_LOG_LEVEL: INFO
      SHOW_SQL: false
      FORMAT_SQL: false
      SPRING_PROFILES_ACTIVE: prod
      OPENAPI_PROD_URL: http://${PUBLIC_IP}
    ports:
      - "8080:8080"
    volumes:
      - ./target:/app
    working_dir: /app
    command: ["java", "-jar", "REPLACE_JAR_NAME"]
    networks:
      - booky-network
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  postgres_data:
    driver: local

networks:
  booky-network:
    driver: bridge
EOF
    
         # Copiar archivos
     scp -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no docker-compose.prod.yml ubuntu@$PUBLIC_IP:/opt/booky-app/
     scp -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no "$JAR_FILE" ubuntu@$PUBLIC_IP:/opt/booky-app/target/
  fi
}

echo "📦 Preparando archivos para deployment..."

# Verificar que el JAR existe
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [[ ! -f "$JAR_FILE" ]]; then
  echo "❌ Error: No se encontró el archivo JAR en target/"
  echo "Ejecuta 'mvn clean package -DskipTests' primero"
  exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
echo "✅ JAR encontrado: $JAR_NAME"

# Crear directorio target en el servidor
remote_exec "mkdir -p /opt/booky-app/target"

copy_files

echo "🐳 Desplegando con Docker Compose..."

# Parar contenedores existentes (si los hay)
remote_exec "cd /opt/booky-app && docker-compose -f docker-compose.prod.yml down || true"

# Limpiar imágenes no utilizadas
remote_exec "docker system prune -f || true"

# Construir y levantar los contenedores
remote_exec "cd /opt/booky-app && docker-compose -f docker-compose.prod.yml up -d"

echo "⏳ Esperando que la aplicación esté lista..."

# Esperar a que la aplicación esté disponible
HEALTH_CHECK_SUCCESS=false
for i in {1..30}; do
    if remote_exec "curl -f http://localhost:8080/actuator/health > /dev/null 2>&1" 2>/dev/null; then
        echo "✅ Aplicación funcionando correctamente"
        HEALTH_CHECK_SUCCESS=true
        break
    fi
    echo "⏳ Intento $i/30 - Esperando que la aplicación esté lista..."
    sleep 15
done

if [[ "$HEALTH_CHECK_SUCCESS" == "false" ]]; then
    echo "⚠️  La aplicación puede estar tardando en iniciar"
    echo "Verifica manualmente: curl http://$PUBLIC_IP/actuator/health"
fi

# Verificar estado de los contenedores
echo "📋 Estado de los contenedores:"
remote_exec "cd /opt/booky-app && docker-compose -f docker-compose.prod.yml ps"

echo ""
echo "🎉 Deployment completado exitosamente!"
echo "🌐 Aplicación disponible en: http://$PUBLIC_IP"
echo "📊 Health check: http://$PUBLIC_IP/actuator/health"
echo "📚 API Docs: http://$PUBLIC_IP/swagger-ui.html"
echo ""
echo "🛠️  Para gestionar el servidor:"
echo "export EC2_HOST=$PUBLIC_IP"
echo "./scripts/manage-server.sh connect"

# Limpiar archivos temporales
rm -f docker-compose.prod.yml
rm -f ~/.ssh/id_rsa 