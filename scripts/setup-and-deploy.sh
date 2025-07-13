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
INSTANCE_RECREATED=false

echo "🚀 Configurando y desplegando Booky Backend en EC2..."

# ================================================================
# PARTE 1: CONFIGURACIÓN DE EC2
# ================================================================

# Función para crear instancia EC2
create_ec2_instance() {
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
  
  # Detectar si estamos en AWS Sandbox/Learner Lab
  USER_ARN=$(aws sts get-caller-identity --query 'Arn' --output text 2>/dev/null || echo "")
  if [[ "$USER_ARN" == *"voclabs"* || "$USER_ARN" == *"student"* || "$USER_ARN" == *"learner"* ]]; then
    echo "🎓 AWS Sandbox/Learner Lab detectado - omitiendo creación de roles IAM"
    IAM_ROLE_NAME=""
    USE_INSTANCE_PROFILE=false
  else
    echo "🔐 AWS cuenta regular - configurando roles IAM..."
    USE_INSTANCE_PROFILE=true
    
    # Crear rol IAM para SSM si no existe
    IAM_ROLE_NAME="EC2-SSM-Role"
    if ! aws iam get-role --role-name $IAM_ROLE_NAME &> /dev/null; then
      echo "🔐 Creando rol IAM para SSM..."
      
      # Crear el rol
      aws iam create-role \
        --role-name $IAM_ROLE_NAME \
        --assume-role-policy-document '{
          "Version": "2012-10-17",
          "Statement": [
            {
              "Effect": "Allow",
              "Principal": {
                "Service": "ec2.amazonaws.com"
              },
              "Action": "sts:AssumeRole"
            }
          ]
        ]' > /dev/null
      
      # Adjuntar la política de SSM
      aws iam attach-role-policy \
        --role-name $IAM_ROLE_NAME \
        --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
      
      # Crear instance profile
      aws iam create-instance-profile --instance-profile-name $IAM_ROLE_NAME > /dev/null
      
      # Adjuntar el rol al instance profile
      aws iam add-role-to-instance-profile \
        --instance-profile-name $IAM_ROLE_NAME \
        --role-name $IAM_ROLE_NAME
      
      # Esperar un poco para que se propaguen los cambios
      sleep 10
      
      echo "✅ Rol IAM creado para SSM"
    fi
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
  if [[ "$USE_INSTANCE_PROFILE" == "true" && ! -z "$IAM_ROLE_NAME" ]]; then
    echo "🔐 Creando instancia con rol IAM para SSM..."
    INSTANCE_ID=$(aws ec2 run-instances \
      --region $REGION \
      --image-id $AMI_ID \
      --instance-type $INSTANCE_TYPE \
      --key-name $KEY_NAME \
      --security-groups $SECURITY_GROUP \
      --iam-instance-profile Name=$IAM_ROLE_NAME \
      --user-data "$USER_DATA" \
      --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME}]" \
      --query 'Instances[0].InstanceId' \
      --output text)
  else
    echo "🎓 Creando instancia para AWS Sandbox (sin rol IAM)..."
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
  fi
  
  echo "⏳ Esperando que la instancia esté lista..."
  aws ec2 wait instance-running --region $REGION --instance-ids $INSTANCE_ID
  
  # Esperar un poco más para que el user-data termine
  echo "⏳ Esperando que la configuración inicial termine..."
  sleep 60
  
  echo "✅ Instancia creada: $INSTANCE_ID"
else
  echo "✅ Instancia ya existe: $INSTANCE_ID"
  
  # Solo verificar SSM si no hemos recreado la instancia ya y estamos en AWS regular
  if [[ "$INSTANCE_RECREATED" == "false" && "$USE_INSTANCE_PROFILE" == "true" ]]; then
    # Verificar si la instancia tiene SSM habilitado
    if ! aws ssm describe-instance-information --region $REGION --filters "Key=InstanceIds,Values=$INSTANCE_ID" --query 'InstanceInformationList[0].InstanceId' --output text 2>/dev/null | grep -q "$INSTANCE_ID"; then
      echo "⚠️  La instancia existente no tiene SSM habilitado"
      echo "💡 Para un deployment más robusto, se recomienda recrear con SSM"
      echo "🔄 Recreando instancia con SSM habilitado..."
      
      # Terminar la instancia existente
      aws ec2 terminate-instances --region $REGION --instance-ids $INSTANCE_ID
      echo "⏳ Esperando que la instancia se termine..."
      aws ec2 wait instance-terminated --region $REGION --instance-ids $INSTANCE_ID
      
      # Marcar como recreada para evitar bucles
      INSTANCE_RECREATED=true
      
      # Recrear la instancia
      echo "🏗️  Recreando instancia EC2 con SSM habilitado..."
      create_ec2_instance
      return
    fi
  elif [[ "$USE_INSTANCE_PROFILE" == "false" ]]; then
    echo "🎓 AWS Sandbox detectado - usando SSH directo (SSM no disponible)"
  fi
fi

# Obtener la IP pública
PUBLIC_IP=$(aws ec2 describe-instances \
  --region $REGION \
  --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo "🌐 IP Pública: $PUBLIC_IP"
}

# Ejecutar creación de instancia
create_ec2_instance

# ================================================================
# PARTE 2: DEPLOYMENT
# ================================================================

echo "🚀 Iniciando deployment de la aplicación..."

# Configurar SSH
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# Crear archivo de llave SSH desde el key pair existente o recién creado
if [[ ! -f "${KEY_NAME}.pem" ]]; then
  echo "🔑 Intentando obtener key pair existente..."
  
  # Verificar si existe un key pair en AWS
  if aws ec2 describe-key-pairs --region $REGION --key-names $KEY_NAME &> /dev/null; then
    echo "⚠️  Key pair existe en AWS pero no tenemos el archivo .pem"
    echo "💡 Para instancias existentes, necesitamos crear un nuevo key pair"
    
    # Crear un nuevo key pair temporal
    TEMP_KEY_NAME="booky-temp-$(date +%s)"
    echo "🔑 Creando key pair temporal: $TEMP_KEY_NAME"
    
    aws ec2 create-key-pair --region $REGION --key-name $TEMP_KEY_NAME --query 'KeyMaterial' --output text > ${TEMP_KEY_NAME}.pem
    chmod 400 ${TEMP_KEY_NAME}.pem
    
    # Agregar la nueva key al autorized_keys de la instancia via user data
    echo "🔧 Agregando nueva key a la instancia..."
    NEW_PUBLIC_KEY=$(ssh-keygen -y -f ${TEMP_KEY_NAME}.pem)
    
    # Usar SSM para agregar la key (si está disponible)
    if aws ssm describe-instance-information --region $REGION --filters "Key=InstanceIds,Values=$INSTANCE_ID" --query 'InstanceInformationList[0].InstanceId' --output text 2>/dev/null | grep -q "$INSTANCE_ID"; then
      echo "🔧 Usando SSM para agregar SSH key..."
      aws ssm send-command \
        --region $REGION \
        --instance-ids $INSTANCE_ID \
        --document-name "AWS-RunShellScript" \
        --parameters "commands=['echo \"$NEW_PUBLIC_KEY\" >> /home/ubuntu/.ssh/authorized_keys']" \
        --query 'Command.CommandId' \
        --output text
      
      # Esperar un poco para que el comando se ejecute
      sleep 10
      
      cp "${TEMP_KEY_NAME}.pem" ~/.ssh/id_rsa
      chmod 600 ~/.ssh/id_rsa
      USE_SSM=false
      KEY_NAME=$TEMP_KEY_NAME
      
      echo "✅ Key SSH agregada exitosamente"
    else
      echo "❌ SSM no está disponible en esta instancia"
      echo "💡 La instancia necesita un rol IAM con permisos SSM"
      echo "🔄 Usando SSH directo como fallback..."
      
      # Usar SSH directo con key temporal
      cp "${TEMP_KEY_NAME}.pem" ~/.ssh/id_rsa
      chmod 600 ~/.ssh/id_rsa
      USE_SSM=false
      KEY_NAME=$TEMP_KEY_NAME
      
      echo "✅ Usando SSH directo con key temporal"
    fi
  else
    echo "❌ Key pair no existe en AWS"
    USE_SSM=false
  fi
else
  cp "${KEY_NAME}.pem" ~/.ssh/id_rsa
  chmod 600 ~/.ssh/id_rsa
  USE_SSM=false
fi

# Determinar método de conexión
if [[ "$USE_INSTANCE_PROFILE" == "false" ]]; then
  echo "🎓 AWS Sandbox: Forzando uso de SSH directo"
  USE_SSM=false
fi

# Función para ejecutar comandos remotos
if [[ "$USE_SSM" == "true" ]]; then
  remote_exec() {
    local cmd="$1"
    echo "🔧 Ejecutando via SSM: $cmd"
    
    # Enviar comando
    local command_id=$(aws ssm send-command \
      --region $REGION \
      --instance-ids $INSTANCE_ID \
      --document-name "AWS-RunShellScript" \
      --parameters "commands=['$cmd']" \
      --query 'Command.CommandId' \
      --output text 2>/dev/null)
    
    if [[ -z "$command_id" ]]; then
      echo "❌ Error enviando comando via SSM"
      return 1
    fi
    
    # Esperar a que el comando termine
    local status="InProgress"
    local attempts=0
    while [[ "$status" == "InProgress" && $attempts -lt 30 ]]; do
      sleep 2
      status=$(aws ssm get-command-invocation \
        --region $REGION \
        --command-id "$command_id" \
        --instance-id $INSTANCE_ID \
        --query 'Status' \
        --output text 2>/dev/null || echo "Failed")
      ((attempts++))
    done
    
    if [[ "$status" == "Success" ]]; then
      return 0
    else
      echo "❌ Comando SSM falló: $status"
      return 1
    fi
  }
  echo "🔧 Usando AWS Systems Manager para deployment"
else
  remote_exec() {
    local cmd="$1"
    echo "🔧 Ejecutando via SSH: $cmd"
    ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no -o ConnectTimeout=10 ubuntu@$PUBLIC_IP "$cmd"
  }
  echo "🔧 Usando SSH para deployment"
  
  # Añadir host a known_hosts
  echo "🔑 Agregando host a known_hosts..."
  ssh-keyscan -H $PUBLIC_IP >> ~/.ssh/known_hosts 2>/dev/null || true
  
  # Probar conexión SSH
  echo "🔍 Probando conexión SSH..."
  if ! ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no -o ConnectTimeout=10 ubuntu@$PUBLIC_IP "echo 'SSH conectado exitosamente'" 2>/dev/null; then
    echo "❌ Error de conexión SSH"
    echo "💡 Intentando usar SSM como fallback..."
    USE_SSM=true
  else
    echo "✅ Conexión SSH exitosa"
  fi
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