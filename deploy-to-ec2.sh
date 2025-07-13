#!/bin/bash

# Script para desplegar booky-backend en EC2
# Ejecutar en el EC2 después de instalar Docker y Docker Compose

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🚀 Iniciando despliegue de Booky Backend en EC2...${NC}"

# Verificar que Docker esté instalado
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker no está instalado. Instalando...${NC}"
    
    # Para Amazon Linux 2
    if [ -f /etc/amazon-linux-release ]; then
        sudo yum update -y
        sudo yum install -y docker
        sudo systemctl start docker
        sudo systemctl enable docker
        sudo usermod -a -G docker ec2-user
        echo -e "${GREEN}✅ Docker instalado en Amazon Linux 2${NC}"
        echo -e "${YELLOW}⚠️ Reinicia la sesión SSH para aplicar permisos de grupo${NC}"
    
    # Para Ubuntu
    elif [ -f /etc/lsb-release ]; then
        sudo apt update
        sudo apt install -y docker.io docker-compose
        sudo systemctl start docker
        sudo systemctl enable docker
        sudo usermod -a -G docker ubuntu
        echo -e "${GREEN}✅ Docker instalado en Ubuntu${NC}"
        echo -e "${YELLOW}⚠️ Reinicia la sesión SSH para aplicar permisos de grupo${NC}"
    else
        echo -e "${RED}❌ Sistema operativo no soportado${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✅ Docker ya está instalado${NC}"
fi

# Verificar que Docker Compose esté instalado
if ! command -v docker-compose &> /dev/null; then
    echo -e "${YELLOW}📦 Instalando Docker Compose...${NC}"
    sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    echo -e "${GREEN}✅ Docker Compose instalado${NC}"
else
    echo -e "${GREEN}✅ Docker Compose ya está instalado${NC}"
fi

# Crear directorio de proyecto
PROJECT_DIR="/home/$(whoami)/booky-backend"
echo -e "${YELLOW}📁 Creando directorio de proyecto: $PROJECT_DIR${NC}"
mkdir -p $PROJECT_DIR
cd $PROJECT_DIR

# Crear archivo .env si no existe
if [ ! -f .env ]; then
    echo -e "${YELLOW}🔧 Creando archivo .env...${NC}"
    cat > .env << 'EOF'
# Database Configuration
POSTGRES_PASSWORD=secure_password_2024

# JWT Configuration
JWT_SECRET=booky_jwt_secret_key_very_secure_32_chars_minimum

# Cloudinary Configuration (opcional - reemplazar con tus valores)
CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

# Spring Profile
SPRING_PROFILES_ACTIVE=local

# Server Configuration
SERVER_PORT=8080
EOF
    echo -e "${GREEN}✅ Archivo .env creado. Edítalo con tus valores reales.${NC}"
fi

# Crear docker-compose para producción
echo -e "${YELLOW}🐳 Creando docker-compose.yml...${NC}"
cat > docker-compose.yml << 'EOF'
services:
  postgres:
    image: postgres:15-alpine
    container_name: booky-postgres-prod
    environment:
      POSTGRES_DB: booky
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-admin}
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
    image: bookypfi/booky-backend:latest
    container_name: booky-backend-prod
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/booky
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: ${POSTGRES_PASSWORD:-admin}
      DATABASE_NAME: booky
      SPRING_PROFILES_ACTIVE: local
      SERVER_PORT: 8080
      JWT_SECRET: ${JWT_SECRET:-your-secret-key-here}
      CLOUDINARY_CLOUD_NAME: ${CLOUDINARY_CLOUD_NAME}
      CLOUDINARY_API_KEY: ${CLOUDINARY_API_KEY}
      CLOUDINARY_API_SECRET: ${CLOUDINARY_API_SECRET}
    ports:
      - "8080:8080"
    networks:
      - booky-network
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
    driver: local

networks:
  booky-network:
    driver: bridge
EOF

echo -e "${GREEN}✅ docker-compose.yml creado${NC}"

# Hacer pull de la imagen
echo -e "${YELLOW}📥 Descargando imagen Docker...${NC}"
docker pull bookypfi/booky-backend:latest

# Iniciar servicios
echo -e "${YELLOW}🚀 Iniciando servicios...${NC}"
docker-compose up -d

# Esperar a que los servicios estén listos
echo -e "${YELLOW}⏳ Esperando a que los servicios estén listos...${NC}"
sleep 30

# Verificar estado de los servicios
echo -e "${YELLOW}📊 Verificando estado de los servicios...${NC}"
docker-compose ps

# Mostrar logs de la aplicación
echo -e "${YELLOW}📋 Últimos logs de la aplicación:${NC}"
docker-compose logs --tail=20 booky-app

# Verificar conectividad
echo -e "${YELLOW}🔍 Verificando conectividad...${NC}"
if curl -f http://localhost:8080/actuator/health &> /dev/null; then
    echo -e "${GREEN}✅ Aplicación funcionando correctamente!${NC}"
    echo -e "${GREEN}🌐 Acceso: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080${NC}"
else
    echo -e "${RED}❌ La aplicación no responde. Verificar logs:${NC}"
    docker-compose logs booky-app
fi

echo -e "${GREEN}🎉 Despliegue completado!${NC}"
echo -e "${YELLOW}📝 Comandos útiles:${NC}"
echo "  docker-compose logs -f booky-app    # Ver logs en tiempo real"
echo "  docker-compose stop                 # Detener servicios"
echo "  docker-compose start                # Iniciar servicios"
echo "  docker-compose down                 # Detener y eliminar containers"
echo "  docker-compose up -d                # Iniciar en modo daemon" 