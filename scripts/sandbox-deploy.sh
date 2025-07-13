#!/bin/bash

# Script optimizado para AWS Learner Lab / Sandbox
# Evita complejidades de IAM y SSM, usa solo funcionalidades básicas

set -e

# Configuración específica para AWS Sandbox
REGION=${AWS_REGION:-us-east-1}
INSTANCE_TYPE=${EC2_INSTANCE_TYPE:-t3.medium}
KEY_NAME="booky-sandbox-key"
SECURITY_GROUP="booky-sandbox-sg"
INSTANCE_NAME="booky-sandbox-server"

echo "🎓 Deployment optimizado para AWS Learner Lab/Sandbox..."

# Verificar que el JAR existe
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [[ ! -f "$JAR_FILE" ]]; then
  echo "❌ Error: No se encontró el archivo JAR en target/"
  echo "Ejecuta 'mvn clean package -DskipTests' primero"
  exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
echo "✅ JAR encontrado: $JAR_NAME"

# Verificar credenciales AWS
USER_ARN=$(aws sts get-caller-identity --query 'Arn' --output text 2>/dev/null || echo "")
if [[ "$USER_ARN" == *"voclabs"* ]]; then
  echo "✅ AWS Learner Lab detectado"
else
  echo "⚠️  Este script está optimizado para AWS Learner Lab"
fi

# Buscar instancia existente
echo "📝 Verificando si existe instancia EC2..."
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
      --description "Security group for Booky Backend in Sandbox" \
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
  
  # Script de inicialización simple
  USER_DATA=$(cat << 'EOF'
#!/bin/bash
# Setup básico para AWS Sandbox
export DEBIAN_FRONTEND=noninteractive

# Actualizar sistema
apt-get update -y

# Instalar Docker
apt-get install -y docker.io curl wget

# Iniciar Docker
systemctl start docker
systemctl enable docker

# Añadir usuario ubuntu al grupo docker
usermod -aG docker ubuntu

# Instalar Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Crear directorios para la aplicación
mkdir -p /opt/booky-app/target
chown -R ubuntu:ubuntu /opt/booky-app

# Instalar nginx
apt-get install -y nginx

# Configurar nginx
cat > /etc/nginx/sites-available/default << 'NGINXCONF'
server {
    listen 80 default_server;
    listen [::]:80 default_server;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINXCONF

# Reiniciar nginx
systemctl restart nginx
systemctl enable nginx

# Marcar setup completo
echo "Setup completo en $(date)" > /tmp/setup-complete.log
EOF
)
  
  # Crear la instancia (SIN rol IAM para Sandbox)
  echo "🚀 Creando instancia EC2 para Sandbox..."
  INSTANCE_ID=$(aws ec2 run-instances \
    --region $REGION \
    --image-id $AMI_ID \
    --instance-type $INSTANCE_TYPE \
    --key-name $KEY_NAME \
    --security-groups $SECURITY_GROUP \
    --user-data "$USER_DATA" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME},{Key=Environment,Value=Sandbox}]" \
    --query 'Instances[0].InstanceId' \
    --output text)
  
  echo "⏳ Esperando que la instancia esté lista..."
  aws ec2 wait instance-running --region $REGION --instance-ids $INSTANCE_ID
  
  # Esperar que el user-data termine
  echo "⏳ Esperando configuración inicial (esto puede tomar 2-3 minutos)..."
  sleep 120
  
  echo "✅ Instancia creada: $INSTANCE_ID"
else
  echo "✅ Instancia existente encontrada: $INSTANCE_ID"
fi

# Obtener IP pública
PUBLIC_IP=$(aws ec2 describe-instances \
  --region $REGION \
  --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo "🌐 IP Pública: $PUBLIC_IP"

# ================================================================
# DEPLOYMENT USANDO SSH DIRECTO
# ================================================================

echo "🚀 Iniciando deployment..."

# Preparar SSH
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# Verificar que tenemos la key SSH
if [[ ! -f "${KEY_NAME}.pem" ]]; then
  echo "❌ Error: No se encontró el archivo ${KEY_NAME}.pem"
  echo "💡 El archivo de key debe estar en el directorio actual"
  echo "🔧 SOLUCIÓN MANUAL:"
  echo "1. Ve a AWS Console → EC2 → Key Pairs"
  echo "2. Descarga la key '${KEY_NAME}'"
  echo "3. Guárdala como '${KEY_NAME}.pem' en este directorio"
  echo "4. Ejecuta: chmod 400 ${KEY_NAME}.pem"
  exit 1
fi

# Configurar SSH
cp "${KEY_NAME}.pem" ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa

# Agregar host a known_hosts
echo "🔑 Configurando SSH..."
ssh-keyscan -H $PUBLIC_IP >> ~/.ssh/known_hosts 2>/dev/null || true

# Probar conexión SSH
echo "🔍 Probando conexión SSH..."
max_attempts=10
attempt=1

while [ $attempt -le $max_attempts ]; do
  if ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no -o ConnectTimeout=10 ubuntu@$PUBLIC_IP "echo 'SSH OK'" 2>/dev/null; then
    echo "✅ Conexión SSH exitosa"
    break
  else
    echo "⏳ Intento $attempt/$max_attempts - Esperando que SSH esté disponible..."
    sleep 15
    ((attempt++))
  fi
done

if [ $attempt -gt $max_attempts ]; then
  echo "❌ No se pudo establecer conexión SSH después de $max_attempts intentos"
  echo "🔧 SOLUCIÓN MANUAL:"
  echo "1. Ve a AWS Console → EC2 → Instances"
  echo "2. Selecciona la instancia: $INSTANCE_ID"
  echo "3. Click 'Connect' → 'EC2 Instance Connect'"
  echo "4. Ejecuta manualmente los comandos de deployment"
  exit 1
fi

# Crear docker-compose de producción
echo "📝 Preparando configuración de producción..."
cat > docker-compose.prod.yml << EOF
version: '3.8'
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
    command: ["java", "-jar", "${JAR_NAME}"]
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

# Preparar servidor
echo "📦 Preparando servidor..."
ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP << 'REMOTE_COMMANDS'
# Asegurar que los directorios existen
sudo mkdir -p /opt/booky-app/target
sudo chown -R ubuntu:ubuntu /opt/booky-app

# Parar contenedores existentes si los hay
cd /opt/booky-app
/usr/local/bin/docker-compose -f docker-compose.prod.yml down 2>/dev/null || true
docker-compose -f docker-compose.prod.yml down 2>/dev/null || true

# Limpiar imágenes no utilizadas
docker system prune -f 2>/dev/null || true

echo "Servidor preparado"
REMOTE_COMMANDS

# Subir archivos
echo "📤 Subiendo archivos..."
scp -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no docker-compose.prod.yml ubuntu@$PUBLIC_IP:/opt/booky-app/
scp -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no "$JAR_FILE" ubuntu@$PUBLIC_IP:/opt/booky-app/target/

# Ejecutar deployment
echo "🐳 Ejecutando deployment..."
ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP << 'DEPLOY_COMMANDS'
cd /opt/booky-app

# Intentar usar docker-compose local primero, luego el instalado globalmente
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif [ -f "/usr/local/bin/docker-compose" ]; then
    DOCKER_COMPOSE_CMD="/usr/local/bin/docker-compose"
else
    echo "Instalando docker-compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    DOCKER_COMPOSE_CMD="/usr/local/bin/docker-compose"
fi

echo "Usando: $DOCKER_COMPOSE_CMD"

# Ejecutar deployment
$DOCKER_COMPOSE_CMD -f docker-compose.prod.yml up -d

echo "Deployment ejecutado"
DEPLOY_COMMANDS

echo "⏳ Esperando que la aplicación esté lista..."
sleep 45

# Verificar deployment
echo "🔍 Verificando deployment..."
attempt=1
max_attempts=12

while [ $attempt -le $max_attempts ]; do
  if curl -f "http://$PUBLIC_IP/actuator/health" 2>/dev/null; then
    echo "✅ Aplicación funcionando correctamente!"
    break
  else
    echo "⏳ Intento $attempt/$max_attempts - Esperando que la aplicación esté lista..."
    sleep 15
    ((attempt++))
  fi
done

# Mostrar estado final
echo "📋 Estado de contenedores:"
ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "cd /opt/booky-app && docker ps"

echo ""
echo "🎉 Deployment completado!"
echo "🌐 Aplicación: http://$PUBLIC_IP"
echo "📊 Health Check: http://$PUBLIC_IP/actuator/health"
echo "📚 API Docs: http://$PUBLIC_IP/swagger-ui.html"
echo ""
echo "🔑 SSH Key guardada: ${KEY_NAME}.pem"
echo "💡 Para conectarte: ssh -i ${KEY_NAME}.pem ubuntu@$PUBLIC_IP"
echo ""

# Limpiar archivos temporales
rm -f docker-compose.prod.yml
rm -f ~/.ssh/id_rsa 