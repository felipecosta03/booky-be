#!/bin/bash

# Script de deployment rápido para instancia EC2 existente
# Diseñado para AWS Sandbox con menos complejidad

set -e

# Configuración
REGION=${AWS_REGION:-us-east-1}
INSTANCE_NAME=${EC2_INSTANCE_NAME:-booky-server}
KEY_NAME=${EC2_KEY_NAME:-booky-key}

echo "🚀 Deployment rápido para instancia existente..."

# Verificar que el JAR existe
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [[ ! -f "$JAR_FILE" ]]; then
  echo "❌ Error: No se encontró el archivo JAR en target/"
  echo "Ejecuta 'mvn clean package -DskipTests' primero"
  exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
echo "✅ JAR encontrado: $JAR_NAME"

# Buscar instancia EC2 existente
echo "📝 Buscando instancia EC2..."
INSTANCE_ID=$(aws ec2 describe-instances \
  --region $REGION \
  --filters "Name=tag:Name,Values=$INSTANCE_NAME" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text 2>/dev/null || echo "None")

if [[ "$INSTANCE_ID" == "None" || "$INSTANCE_ID" == "" ]]; then
  echo "❌ No se encontró instancia EC2 activa"
  echo "💡 Ejecuta primero: ./scripts/setup-and-deploy.sh"
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

# Intentar conectar con key existente
echo "🔑 Intentando conectar con keys existentes..."
SSH_KEY_FOUND=false
SSH_KEY_FILE=""

# Buscar keys existentes
for key_file in *.pem; do
  if [[ -f "$key_file" ]]; then
    echo "🔍 Probando key: $key_file"
    if ssh -i "$key_file" -o StrictHostKeyChecking=no -o ConnectTimeout=5 ubuntu@$PUBLIC_IP "echo 'Conexión exitosa'" 2>/dev/null; then
      echo "✅ Conexión exitosa con $key_file"
      SSH_KEY_FOUND=true
      SSH_KEY_FILE="$key_file"
      break
    fi
  fi
done

if [[ "$SSH_KEY_FOUND" == "false" ]]; then
  echo "❌ No se pudo conectar con keys existentes"
  echo ""
  echo "🔧 SOLUCIÓN MANUAL:"
  echo "1. Ve a la consola AWS EC2"
  echo "2. Selecciona la instancia: $INSTANCE_ID"
  echo "3. Click 'Connect' → 'EC2 Instance Connect'"
  echo "4. Ejecuta estos comandos:"
  echo ""
  echo "   # Preparar directorios"
  echo "   sudo mkdir -p /opt/booky-app/target"
  echo "   sudo chown -R ubuntu:ubuntu /opt/booky-app"
  echo "   cd /opt/booky-app"
  echo ""
  echo "   # Descargar y subir archivos manualmente"
  echo "   # Luego ejecutar:"
  echo "   docker-compose -f docker-compose.prod.yml down 2>/dev/null || true"
  echo "   docker-compose -f docker-compose.prod.yml up -d"
  echo ""
  echo "📁 Archivos que necesitas subir:"
  echo "   - docker-compose.prod.yml (se generará a continuación)"
  echo "   - target/$JAR_NAME"
  echo ""
  
  # Generar docker-compose.prod.yml
  echo "📝 Generando docker-compose.prod.yml..."
  cat > docker-compose.prod.yml << EOF
services:
  postgres:
    image: postgres:15-alpine
    container_name: booky-postgres
    environment:
      POSTGRES_DB: booky
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: \${DATABASE_PASSWORD}
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
      DATABASE_PASSWORD: \${DATABASE_PASSWORD}
      DATABASE_NAME: booky
      DDL_AUTO: update
      JWT_SECRET: \${JWT_SECRET}
      JWT_EXPIRATION: 86400000
      CLOUDINARY_CLOUD_NAME: \${CLOUDINARY_CLOUD_NAME}
      CLOUDINARY_API_KEY: \${CLOUDINARY_API_KEY}
      CLOUDINARY_API_SECRET: \${CLOUDINARY_API_SECRET}
      CORS_ALLOWED_ORIGINS: http://localhost:3000,http://$PUBLIC_IP
      CORS_ALLOWED_METHODS: GET,POST,PUT,DELETE,OPTIONS
      CORS_ALLOWED_HEADERS: "*"
      CORS_ALLOW_CREDENTIALS: true
      LOG_LEVEL: INFO
      APP_LOG_LEVEL: INFO
      SECURITY_LOG_LEVEL: INFO
      SHOW_SQL: false
      FORMAT_SQL: false
      SPRING_PROFILES_ACTIVE: prod
      OPENAPI_PROD_URL: http://$PUBLIC_IP
    ports:
      - "8080:8080"
    volumes:
      - ./target:/app
    working_dir: /app
    command: ["java", "-jar", "$JAR_NAME"]
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
  
  echo "✅ Archivo docker-compose.prod.yml generado"
  echo ""
  echo "🔧 Para deployment manual:"
  echo "1. Sube docker-compose.prod.yml a /opt/booky-app/"
  echo "2. Sube $JAR_FILE a /opt/booky-app/target/"
  echo "3. Ejecuta: docker-compose -f docker-compose.prod.yml up -d"
  echo ""
  exit 1
fi

# Deployment automático
echo "🚀 Ejecutando deployment automático..."

# Crear docker-compose de producción con variables reemplazadas
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
ssh -i "$SSH_KEY_FILE" -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "
  sudo mkdir -p /opt/booky-app/target
  sudo chown -R ubuntu:ubuntu /opt/booky-app
  cd /opt/booky-app
  docker-compose -f docker-compose.prod.yml down 2>/dev/null || true
"

# Subir archivos
echo "📤 Subiendo archivos..."
scp -i "$SSH_KEY_FILE" -o StrictHostKeyChecking=no docker-compose.prod.yml ubuntu@$PUBLIC_IP:/opt/booky-app/
scp -i "$SSH_KEY_FILE" -o StrictHostKeyChecking=no "$JAR_FILE" ubuntu@$PUBLIC_IP:/opt/booky-app/target/

# Ejecutar deployment
echo "🐳 Ejecutando deployment..."
ssh -i "$SSH_KEY_FILE" -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "
  cd /opt/booky-app
  docker-compose -f docker-compose.prod.yml up -d
"

echo "⏳ Esperando que la aplicación esté lista..."
sleep 30

# Verificar deployment
echo "🔍 Verificando deployment..."
if ssh -i "$SSH_KEY_FILE" -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "curl -f http://localhost:8080/actuator/health" 2>/dev/null; then
  echo "✅ Deployment exitoso!"
else
  echo "⚠️  La aplicación puede estar iniciando..."
  echo "💡 Verifica manualmente: http://$PUBLIC_IP/actuator/health"
fi

# Mostrar estado
echo "📋 Estado de contenedores:"
ssh -i "$SSH_KEY_FILE" -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "cd /opt/booky-app && docker-compose -f docker-compose.prod.yml ps"

echo ""
echo "🎉 Deployment completado!"
echo "🌐 Aplicación: http://$PUBLIC_IP"
echo "📊 Health Check: http://$PUBLIC_IP/actuator/health"
echo "📚 API Docs: http://$PUBLIC_IP/swagger-ui.html"
echo ""

# Limpiar archivos temporales
rm -f docker-compose.prod.yml 