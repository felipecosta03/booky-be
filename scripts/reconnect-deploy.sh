#!/bin/bash

# Script para reconectarse y completar deployment en instancia existente
# Especialmente útil cuando la instancia ya se creó pero SSH tardó en estar listo

set -e

# Configuración
REGION=${AWS_REGION:-us-east-1}
INSTANCE_NAME="booky-sandbox-server"
KEY_NAME="booky-sandbox-key"

echo "🔄 Reconectando para completar deployment..."

# Verificar que el JAR existe
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [[ ! -f "$JAR_FILE" ]]; then
  echo "❌ Error: No se encontró el archivo JAR en target/"
  echo "Ejecuta 'mvn clean package -DskipTests' primero"
  exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
echo "✅ JAR encontrado: $JAR_NAME"

# Buscar instancia existente
echo "📝 Buscando instancia existente..."
INSTANCE_ID=$(aws ec2 describe-instances \
  --region $REGION \
  --filters "Name=tag:Name,Values=$INSTANCE_NAME" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text 2>/dev/null || echo "None")

if [[ "$INSTANCE_ID" == "None" || "$INSTANCE_ID" == "" ]]; then
  echo "❌ No se encontró instancia existente"
  echo "💡 Ejecuta primero: ./scripts/sandbox-deploy.sh"
  exit 1
fi

# Obtener IP pública
PUBLIC_IP=$(aws ec2 describe-instances \
  --region $REGION \
  --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo "✅ Instancia encontrada: $INSTANCE_ID"
echo "🌐 IP Pública: $PUBLIC_IP"

# Verificar que tenemos la SSH key
if [[ ! -f "${KEY_NAME}.pem" ]]; then
  echo "❌ Error: No se encontró el archivo ${KEY_NAME}.pem"
  echo "💡 Asegúrate de que el archivo de key esté en el directorio actual"
  exit 1
fi

# Configurar SSH
mkdir -p ~/.ssh
chmod 700 ~/.ssh
cp "${KEY_NAME}.pem" ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa

# Agregar host a known_hosts
echo "🔑 Configurando SSH..."
ssh-keyscan -H $PUBLIC_IP >> ~/.ssh/known_hosts 2>/dev/null || true

# Probar conexión SSH con más intentos y tiempo
echo "🔍 Probando conexión SSH (con paciencia extra)..."
max_attempts=20
attempt=1

while [ $attempt -le $max_attempts ]; do
  if ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no -o ConnectTimeout=10 ubuntu@$PUBLIC_IP "echo 'SSH OK'" 2>/dev/null; then
    echo "✅ Conexión SSH exitosa!"
    break
  else
    echo "⏳ Intento $attempt/$max_attempts - La instancia se está configurando..."
    if [ $attempt -eq 10 ]; then
      echo "💡 La instancia puede estar instalando Docker y otras dependencias..."
    fi
    sleep 30  # Esperar más tiempo entre intentos
    ((attempt++))
  fi
done

if [ $attempt -gt $max_attempts ]; then
  echo "❌ SSH no disponible después de $max_attempts intentos"
  echo ""
  echo "🔧 OPCIONES:"
  echo "1. **Esperar más y reintentar** (la instancia puede necesitar más tiempo)"
  echo "   ./scripts/reconnect-deploy.sh"
  echo ""
  echo "2. **Usar AWS Console** para conectarse:"
  echo "   - Ve a AWS Console → EC2 → Instances"
  echo "   - Selecciona: $INSTANCE_ID"
  echo "   - Click 'Connect' → 'EC2 Instance Connect'"
  echo ""
  echo "3. **Verificar logs de la instancia** en AWS Console"
  exit 1
fi

# Verificar que Docker está funcionando
echo "🐳 Verificando Docker en el servidor..."
if ! ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "docker --version" 2>/dev/null; then
  echo "⏳ Docker aún se está instalando... esperando 60 segundos más"
  sleep 60
  
  if ! ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "docker --version" 2>/dev/null; then
    echo "❌ Docker no está disponible aún"
    echo "💡 La instancia puede necesitar más tiempo de configuración"
    echo "🔄 Intenta ejecutar este script nuevamente en unos minutos"
    exit 1
  fi
fi

echo "✅ Docker está funcionando"

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
if [ -f docker-compose.prod.yml ]; then
  docker-compose -f docker-compose.prod.yml down 2>/dev/null || true
fi

# Limpiar imágenes no utilizadas
docker system prune -f 2>/dev/null || true

echo "Servidor preparado"
REMOTE_COMMANDS

# Subir archivos
echo "📤 Subiendo archivos..."
scp -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no docker-compose.prod.yml ubuntu@$PUBLIC_IP:/opt/booky-app/
scp -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no "$JAR_FILE" ubuntu@$PUBLIC_IP:/opt/booky-app/target/

# Verificar que docker-compose está disponible
echo "🔧 Verificando Docker Compose..."
ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP << 'SETUP_COMMANDS'
# Verificar si docker-compose está disponible
if command -v docker-compose &> /dev/null; then
    echo "Docker Compose ya está instalado"
elif [ -f "/usr/local/bin/docker-compose" ]; then
    echo "Docker Compose está en /usr/local/bin/"
else
    echo "Instalando Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi
SETUP_COMMANDS

# Ejecutar deployment
echo "🐳 Ejecutando deployment..."
ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP << 'DEPLOY_COMMANDS'
cd /opt/booky-app

# Determinar qué comando usar para docker-compose
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif [ -f "/usr/local/bin/docker-compose" ]; then
    DOCKER_COMPOSE_CMD="/usr/local/bin/docker-compose"
else
    echo "❌ Docker Compose no está disponible"
    exit 1
fi

echo "Usando: $DOCKER_COMPOSE_CMD"

# Ejecutar deployment
$DOCKER_COMPOSE_CMD -f docker-compose.prod.yml up -d

echo "✅ Contenedores iniciados"
DEPLOY_COMMANDS

echo "⏳ Esperando que la aplicación esté lista..."
sleep 60

# Verificar deployment
echo "🔍 Verificando deployment..."
attempt=1
max_attempts=15

while [ $attempt -le $max_attempts ]; do
  if curl -f "http://$PUBLIC_IP/actuator/health" 2>/dev/null; then
    echo "✅ ¡Aplicación funcionando correctamente!"
    break
  else
    echo "⏳ Intento $attempt/$max_attempts - Esperando que la aplicación esté lista..."
    sleep 20
    ((attempt++))
  fi
done

if [ $attempt -gt $max_attempts ]; then
  echo "⚠️  La aplicación puede estar iniciando aún..."
  echo "🔍 Verificando estado de contenedores..."
  ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "cd /opt/booky-app && docker ps"
  echo ""
  echo "📋 Para verificar logs:"
  echo "ssh -i ${KEY_NAME}.pem ubuntu@$PUBLIC_IP 'cd /opt/booky-app && docker-compose -f docker-compose.prod.yml logs'"
else
  # Mostrar estado final
  echo "📋 Estado de contenedores:"
  ssh -i ~/.ssh/id_rsa -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "cd /opt/booky-app && docker ps"
fi

echo ""
echo "🎉 Deployment completado!"
echo "🌐 Aplicación: http://$PUBLIC_IP"
echo "📊 Health Check: http://$PUBLIC_IP/actuator/health"
echo "📚 API Docs: http://$PUBLIC_IP/swagger-ui.html"
echo ""
echo "🔑 SSH Key: ${KEY_NAME}.pem"
echo "💡 Para conectarte: ssh -i ${KEY_NAME}.pem ubuntu@$PUBLIC_IP"
echo ""

# Limpiar archivos temporales
rm -f docker-compose.prod.yml
rm -f ~/.ssh/id_rsa
</rewritten_file> 