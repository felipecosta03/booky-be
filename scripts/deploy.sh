#!/bin/bash

# Script de deployment para Booky Backend
# Se ejecuta desde GitHub Actions y despliega la aplicación en EC2

set -e

echo "🚀 Iniciando deployment de Booky Backend..."

# Configurar SSH
mkdir -p ~/.ssh
echo "$EC2_KEY" > ~/.ssh/id_rsa
chmod 600 ~/.ssh/id_rsa
ssh-keyscan -H $EC2_HOST >> ~/.ssh/known_hosts

# Función para ejecutar comandos en el servidor remoto
remote_exec() {
    ssh -i ~/.ssh/id_rsa $EC2_USER@$EC2_HOST "$1"
}

# Función para copiar archivos al servidor remoto
remote_copy() {
    scp -i ~/.ssh/id_rsa -r "$1" $EC2_USER@$EC2_HOST:"$2"
}

echo "📦 Copiando archivos al servidor..."

# Crear directorio de aplicación si no existe
remote_exec "mkdir -p /opt/booky-app"

# Copiar archivos necesarios
remote_copy "docker-compose.yml" "/opt/booky-app/"
remote_copy "scripts/" "/opt/booky-app/"
remote_copy "target/*.jar" "/opt/booky-app/"

# Crear docker-compose.prod.yml optimizado para producción
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
      - ./scripts/database_schema_updated.sql:/docker-entrypoint-initdb.d/00-schema.sql
      - ./scripts/alta_usuarios.sql:/docker-entrypoint-initdb.d/01-users.sql
      - ./scripts/alta_comunidades.sql:/docker-entrypoint-initdb.d/02-communities.sql
      - ./scripts/alta_clubes_lectura.sql:/docker-entrypoint-initdb.d/03-reading-clubs.sql
    networks:
      - booky-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d booky"]
      interval: 30s
      timeout: 10s
      retries: 3

  booky-app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: booky-backend
    environment:
      # Database Configuration
      DATABASE_URL: jdbc:postgresql://postgres:5432/booky
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      DATABASE_NAME: booky
      DDL_AUTO: update
      
      # Security Configuration
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: 86400000
      
      # Cloudinary Configuration
      CLOUDINARY_CLOUD_NAME: ${CLOUDINARY_CLOUD_NAME}
      CLOUDINARY_API_KEY: ${CLOUDINARY_API_KEY}
      CLOUDINARY_API_SECRET: ${CLOUDINARY_API_SECRET}
      
      # CORS Configuration
      CORS_ALLOWED_ORIGINS: http://localhost:3000,https://${EC2_HOST}
      CORS_ALLOWED_METHODS: GET,POST,PUT,DELETE,OPTIONS
      CORS_ALLOWED_HEADERS: "*"
      CORS_ALLOW_CREDENTIALS: true
      
      # Logging Configuration
      LOG_LEVEL: INFO
      APP_LOG_LEVEL: INFO
      SECURITY_LOG_LEVEL: INFO
      SHOW_SQL: false
      FORMAT_SQL: false
      
      # Spring Profiles
      SPRING_PROFILES_ACTIVE: prod
      
      # OpenAPI Configuration
      OPENAPI_PROD_URL: https://${EC2_HOST}
    ports:
      - "8080:8080"
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

# Copiar docker-compose de producción
remote_copy "docker-compose.prod.yml" "/opt/booky-app/"

# Crear archivo .env en el servidor
cat > .env.prod << EOF
DATABASE_URL=jdbc:postgresql://postgres:5432/booky
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=${DATABASE_PASSWORD}
DATABASE_NAME=booky
DDL_AUTO=update

CLOUDINARY_CLOUD_NAME=${CLOUDINARY_CLOUD_NAME}
CLOUDINARY_API_KEY=${CLOUDINARY_API_KEY}
CLOUDINARY_API_SECRET=${CLOUDINARY_API_SECRET}

JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=86400000

LOG_LEVEL=INFO
APP_LOG_LEVEL=INFO
SECURITY_LOG_LEVEL=INFO
SHOW_SQL=false
FORMAT_SQL=false

CORS_ALLOWED_ORIGINS=http://localhost:3000,https://${EC2_HOST}
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true

SPRING_PROFILES_ACTIVE=prod

OPENAPI_PROD_URL=https://${EC2_HOST}
EOF

remote_copy ".env.prod" "/opt/booky-app/.env"

echo "🐳 Desplegando con Docker Compose..."

# Parar contenedores existentes (si los hay)
remote_exec "cd /opt/booky-app && docker-compose -f docker-compose.prod.yml down || true"

# Limpiar imágenes no utilizadas
remote_exec "docker system prune -f || true"

# Construir y levantar los contenedores
remote_exec "cd /opt/booky-app && docker-compose -f docker-compose.prod.yml up -d --build"

echo "⏳ Esperando que la aplicación esté lista..."

# Esperar a que la aplicación esté disponible
for i in {1..30}; do
    if remote_exec "curl -f http://localhost:8080/actuator/health > /dev/null 2>&1" 2>/dev/null; then
        echo "✅ Aplicación funcionando correctamente"
        break
    fi
    echo "⏳ Intento $i/30 - Esperando que la aplicación esté lista..."
    sleep 10
done

# Verificar estado de los contenedores
echo "📋 Estado de los contenedores:"
remote_exec "cd /opt/booky-app && docker-compose -f docker-compose.prod.yml ps"

echo "🎉 Deployment completado exitosamente!"
echo "🌐 Aplicación disponible en: http://$EC2_HOST"
echo "📊 Health check: http://$EC2_HOST/actuator/health"
echo "📚 API Docs: http://$EC2_HOST/swagger-ui.html"

# Limpiar archivos temporales
rm -f ~/.ssh/id_rsa
rm -f docker-compose.prod.yml
rm -f .env.prod 