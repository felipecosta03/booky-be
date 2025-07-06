#!/bin/bash

# Script simplificado para configurar AWS Sandbox con credenciales temporales
# Optimizado para AWS Learner Lab y otros entornos sandbox

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para mostrar el banner
show_banner() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════════════════════════════╗"
    echo "║                                                                                      ║"
    echo "║                    🚀 BOOKY BACKEND - AWS SANDBOX SETUP                             ║"
    echo "║                                                                                      ║"
    echo "║                   Configuración rápida para AWS Learner Lab                         ║"
    echo "║                                                                                      ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Función para mostrar instrucciones
show_instructions() {
    echo -e "${BLUE}📋 Instrucciones para obtener credenciales de AWS Sandbox:${NC}"
    echo ""
    echo "1. Abre AWS Learner Lab (o tu sandbox)"
    echo "2. Haz clic en 'Start Lab' si no está iniciado"
    echo "3. Haz clic en 'AWS Details'"
    echo "4. Haz clic en 'AWS CLI'"
    echo "5. Copia el contenido que aparece (incluye Access Key, Secret Key y Session Token)"
    echo ""
    echo -e "${YELLOW}El formato debe ser algo como:${NC}"
    echo "aws_access_key_id=AKIAIOSFODNN7EXAMPLE"
    echo "aws_secret_access_key=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
    echo "aws_session_token=AQoEXAMPLEH4aoAH0gNCAPyJxz4BlCFFxWNE1OPTgk6TQ..."
    echo ""
    echo "Presiona Enter para continuar..."
    read
}

# Función para configurar credenciales
setup_credentials() {
    echo -e "${BLUE}🔑 Configurando credenciales de AWS Sandbox...${NC}"
    echo ""
    
    # Verificar si ya hay credenciales configuradas
    if aws sts get-caller-identity &> /dev/null; then
        echo -e "${GREEN}✅ Ya hay credenciales configuradas${NC}"
        echo "Configuración actual:"
        aws sts get-caller-identity
        echo ""
        read -p "¿Deseas usar las credenciales actuales? (y/n): " use_current
        if [[ "$use_current" == "y" || "$use_current" == "Y" ]]; then
            return 0
        fi
    fi
    
    echo "Ingresa tus credenciales de AWS Sandbox:"
    echo ""
    
    read -p "AWS Access Key ID: " AWS_ACCESS_KEY_ID
    read -p "AWS Secret Access Key: " AWS_SECRET_ACCESS_KEY
    read -p "AWS Session Token: " AWS_SESSION_TOKEN
    read -p "AWS Region (default: us-east-1): " AWS_REGION
    AWS_REGION=${AWS_REGION:-us-east-1}
    
    # Configurar variables de entorno
    export AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID"
    export AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY"
    export AWS_SESSION_TOKEN="$AWS_SESSION_TOKEN"
    export AWS_DEFAULT_REGION="$AWS_REGION"
    
    # Configurar archivos de AWS
    mkdir -p ~/.aws
    cat > ~/.aws/credentials << EOF
[default]
aws_access_key_id = $AWS_ACCESS_KEY_ID
aws_secret_access_key = $AWS_SECRET_ACCESS_KEY
aws_session_token = $AWS_SESSION_TOKEN
EOF
    
    cat > ~/.aws/config << EOF
[default]
region = $AWS_REGION
output = json
EOF
    
    # Verificar credenciales
    echo -e "${BLUE}🔍 Verificando credenciales...${NC}"
    if aws sts get-caller-identity &> /dev/null; then
        echo -e "${GREEN}✅ Credenciales configuradas correctamente${NC}"
        echo "Usuario: $(aws sts get-caller-identity --query 'Arn' --output text)"
        echo "Región: $AWS_REGION"
    else
        echo -e "${RED}❌ Error al verificar credenciales${NC}"
        echo "Verifica que las credenciales estén correctas y que no hayan expirado"
        exit 1
    fi
}

# Función para generar secrets
generate_secrets() {
    echo -e "${BLUE}🔐 Generando secrets para la aplicación...${NC}"
    
    # Generar JWT Secret
    JWT_SECRET=$(openssl rand -base64 32)
    echo -e "${GREEN}✅ JWT Secret generado${NC}"
    
    # Generar Database Password
    DATABASE_PASSWORD=$(openssl rand -base64 16 | tr -d '/+=' | cut -c1-16)
    echo -e "${GREEN}✅ Database Password generado${NC}"
    
    echo -e "${GREEN}✅ Secrets generados correctamente${NC}"
}

# Función para configurar Cloudinary
setup_cloudinary() {
    echo -e "${BLUE}☁️  Configurando Cloudinary (opcional)...${NC}"
    echo ""
    echo "Cloudinary es para subir imágenes. Puedes configurarlo después si quieres."
    echo ""
    
    read -p "¿Deseas configurar Cloudinary ahora? (y/n): " setup_cloudinary
    if [[ "$setup_cloudinary" == "y" || "$setup_cloudinary" == "Y" ]]; then
        echo "Obtén tus credenciales en: https://cloudinary.com/console"
        echo ""
        read -p "Cloudinary Cloud Name: " CLOUDINARY_CLOUD_NAME
        read -p "Cloudinary API Key: " CLOUDINARY_API_KEY
        read -p "Cloudinary API Secret: " CLOUDINARY_API_SECRET
        echo -e "${GREEN}✅ Cloudinary configurado${NC}"
    else
        CLOUDINARY_CLOUD_NAME="your_cloud_name"
        CLOUDINARY_API_KEY="your_api_key"
        CLOUDINARY_API_SECRET="your_api_secret"
        echo -e "${YELLOW}⚠️  Cloudinary no configurado (configúralo después en GitHub Secrets)${NC}"
    fi
}

# Función para mostrar GitHub Secrets
show_github_secrets() {
    echo -e "${BLUE}📋 Configuración de GitHub Secrets${NC}"
    echo ""
    echo "Necesitas configurar estos secrets en GitHub:"
    echo "Repository → Settings → Secrets and variables → Actions"
    echo ""
    echo -e "${YELLOW}Copia exactamente estos valores:${NC}"
    echo ""
    echo "AWS_ACCESS_KEY_ID"
    echo "$AWS_ACCESS_KEY_ID"
    echo ""
    echo "AWS_SECRET_ACCESS_KEY"
    echo "$AWS_SECRET_ACCESS_KEY"
    echo ""
    echo "AWS_SESSION_TOKEN"
    echo "$AWS_SESSION_TOKEN"
    echo ""
    echo "AWS_REGION"
    echo "$AWS_REGION"
    echo ""
    echo "DATABASE_PASSWORD"
    echo "$DATABASE_PASSWORD"
    echo ""
    echo "JWT_SECRET"
    echo "$JWT_SECRET"
    echo ""
    echo "CLOUDINARY_CLOUD_NAME"
    echo "$CLOUDINARY_CLOUD_NAME"
    echo ""
    echo "CLOUDINARY_API_KEY"
    echo "$CLOUDINARY_API_KEY"
    echo ""
    echo "CLOUDINARY_API_SECRET"
    echo "$CLOUDINARY_API_SECRET"
    echo ""
    
    # Guardar en archivo
    cat > github-secrets-sandbox.txt << EOF
# GitHub Secrets para Booky Backend - AWS Sandbox
# Copia estos valores exactamente en GitHub → Settings → Secrets and variables → Actions

AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
AWS_SESSION_TOKEN=$AWS_SESSION_TOKEN
AWS_REGION=$AWS_REGION
DATABASE_PASSWORD=$DATABASE_PASSWORD
JWT_SECRET=$JWT_SECRET
CLOUDINARY_CLOUD_NAME=$CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY=$CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET=$CLOUDINARY_API_SECRET

# Nota: Las credenciales de sandbox expiran. 
# Si el deployment falla, regenera las credenciales y actualiza los secrets.
EOF
    
    echo -e "${GREEN}✅ Secrets guardados en github-secrets-sandbox.txt${NC}"
    echo -e "${YELLOW}⚠️  IMPORTANTE: Este archivo NO se subirá a GitHub (está en .gitignore)${NC}"
}

# Función para crear .env local
create_local_env() {
    echo -e "${BLUE}📄 Creando archivo .env para desarrollo local...${NC}"
    
    cat > .env << EOF
# Booky Backend - Local Development
# Para usar con docker-compose localmente

# Database Configuration
DATABASE_URL=jdbc:postgresql://localhost:5433/booky
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=admin
DATABASE_NAME=booky
DDL_AUTO=update

# Cloudinary Configuration
CLOUDINARY_CLOUD_NAME=$CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY=$CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET=$CLOUDINARY_API_SECRET

# Security Configuration
JWT_SECRET=$JWT_SECRET
JWT_EXPIRATION=86400000

# Logging Configuration
LOG_LEVEL=INFO
APP_LOG_LEVEL=DEBUG
SECURITY_LOG_LEVEL=INFO
SHOW_SQL=true
FORMAT_SQL=true

# CORS Configuration
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
CORS_ALLOWED_METHODS=GET,POST,PUT,DELETE,OPTIONS
CORS_ALLOWED_HEADERS=*
CORS_ALLOW_CREDENTIALS=true

# Spring Profiles
SPRING_PROFILES_ACTIVE=local

# OpenAPI Configuration
OPENAPI_DEV_URL=http://localhost:8080
OPENAPI_PROD_URL=https://your-production-url.com
EOF
    
    echo -e "${GREEN}✅ Archivo .env creado${NC}"
}

# Función para mostrar próximos pasos
show_next_steps() {
    echo -e "${BLUE}🎯 Próximos pasos:${NC}"
    echo ""
    echo "1. ✅ Configura los secrets en GitHub usando el archivo github-secrets-sandbox.txt"
    echo "2. 🚀 Haz push a tu repositorio:"
    echo "   git add ."
    echo "   git commit -m 'Setup AWS Sandbox deployment'"
    echo "   git push origin main"
    echo ""
    echo "3. 🔍 Ve a GitHub Actions para monitorear el deployment"
    echo "4. 🌐 Cuando termine, tu app estará en: http://TU_IP_PUBLICA"
    echo ""
    echo -e "${BLUE}📋 El deployment automáticamente:${NC}"
    echo "- Construye la aplicación (sin tests para evitar problemas de DB)"
    echo "- Crea la instancia EC2 si no existe"
    echo "- Despliega con Docker Compose"
    echo "- Configura Nginx como reverse proxy"
    echo ""
    echo -e "${YELLOW}⚠️  IMPORTANTE para AWS Sandbox:${NC}"
    echo "- Las credenciales expiran cuando se cierra el lab"
    echo "- Si el deployment falla por credenciales expiradas:"
    echo "  1. Ejecuta este script nuevamente"
    echo "  2. Actualiza los secrets en GitHub"
    echo "  3. Vuelve a hacer push"
    echo ""
    echo -e "${GREEN}¡Listo! Tu aplicación se desplegará automáticamente 🎉${NC}"
    echo ""
    echo -e "${YELLOW}🔒 RECORDATORIO DE SEGURIDAD:${NC}"
    echo "Los siguientes archivos NO se subirán a GitHub (están en .gitignore):"
    echo "- github-secrets-sandbox.txt"
    echo "- .env"
    echo "- *.pem"
    echo "- archivos con credenciales"
}

# Función principal
main() {
    show_banner
    show_instructions
    setup_credentials
    generate_secrets
    setup_cloudinary
    show_github_secrets
    create_local_env
    show_next_steps
}

# Ejecutar
main "$@" 