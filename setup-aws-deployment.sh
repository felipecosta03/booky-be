#!/bin/bash

# Script de configuración inicial para AWS Deployment
# Facilita la configuración de GitHub Secrets y credenciales AWS

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
    echo "║                    🚀 BOOKY BACKEND AWS DEPLOYMENT SETUP                            ║"
    echo "║                                                                                      ║"
    echo "║                   Configuración automática para EC2 con Docker                      ║"
    echo "║                                                                                      ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Función para mostrar ayuda
show_help() {
    echo -e "${BLUE}📋 Este script te ayudará a configurar el deployment automático de Booky Backend en AWS EC2${NC}"
    echo ""
    echo "El script realizará las siguientes tareas:"
    echo "1. ✅ Verificar que tienes las herramientas necesarias instaladas"
    echo "2. ✅ Configurar credenciales de AWS"
    echo "3. ✅ Generar los secrets necesarios"
    echo "4. ✅ Proporcionar instrucciones para configurar GitHub Secrets"
    echo "5. ✅ Verificar que todo está configurado correctamente"
    echo ""
    echo "Requisitos previos:"
    echo "- Cuenta de AWS con permisos para crear EC2 instances"
    echo "- Repository en GitHub"
    echo "- Credenciales de Cloudinary (opcional)"
    echo ""
    echo "Presiona Enter para continuar o Ctrl+C para salir..."
    read
}

# Función para verificar herramientas instaladas
check_tools() {
    echo -e "${BLUE}🔍 Verificando herramientas necesarias...${NC}"
    
    TOOLS_OK=true
    
    # Verificar AWS CLI
    if ! command -v aws &> /dev/null; then
        echo -e "${RED}❌ AWS CLI no está instalado${NC}"
        echo "Instala AWS CLI: https://docs.aws.amazon.com/cli/latest/userguide/install-cliv2.html"
        TOOLS_OK=false
    else
        echo -e "${GREEN}✅ AWS CLI instalado${NC}"
    fi
    
    # Verificar jq
    if ! command -v jq &> /dev/null; then
        echo -e "${YELLOW}⚠️  jq no está instalado (recomendado para parsear JSON)${NC}"
        echo "Instala jq: sudo apt-get install jq (Ubuntu/Debian) o brew install jq (macOS)"
    else
        echo -e "${GREEN}✅ jq instalado${NC}"
    fi
    
    # Verificar openssl
    if ! command -v openssl &> /dev/null; then
        echo -e "${RED}❌ openssl no está instalado${NC}"
        TOOLS_OK=false
    else
        echo -e "${GREEN}✅ openssl instalado${NC}"
    fi
    
    if [[ "$TOOLS_OK" == false ]]; then
        echo -e "${RED}❌ Instala las herramientas necesarias antes de continuar${NC}"
        exit 1
    fi
}

# Función para configurar AWS credentials (incluyendo sandbox)
setup_aws_credentials() {
    echo -e "${BLUE}🔑 Configurando credenciales de AWS...${NC}"
    echo ""
    
    # Verificar si AWS CLI ya está configurado
    if aws sts get-caller-identity &> /dev/null; then
        echo -e "${GREEN}✅ AWS CLI ya está configurado${NC}"
        echo "Configuración actual:"
        aws sts get-caller-identity
        echo ""
        read -p "¿Deseas usar esta configuración? (y/n): " use_current
        if [[ "$use_current" == "y" || "$use_current" == "Y" ]]; then
            return 0
        fi
    fi
    
    echo "Configuración para AWS Sandbox con credenciales temporales."
    echo "Puedes obtenerlas en: AWS Learner Lab → AWS Details → AWS CLI"
    echo ""
    echo "Copia y pega las credenciales desde AWS CLI:"
    echo ""
    
    read -p "AWS Access Key ID: " AWS_ACCESS_KEY_ID
    read -p "AWS Secret Access Key: " AWS_SECRET_ACCESS_KEY
    read -p "AWS Session Token: " AWS_SESSION_TOKEN
    read -p "AWS Region (default: us-east-1): " AWS_REGION
    AWS_REGION=${AWS_REGION:-us-east-1}
    
    # Configurar AWS CLI con session token
    export AWS_ACCESS_KEY_ID="$AWS_ACCESS_KEY_ID"
    export AWS_SECRET_ACCESS_KEY="$AWS_SECRET_ACCESS_KEY"
    export AWS_SESSION_TOKEN="$AWS_SESSION_TOKEN"
    export AWS_DEFAULT_REGION="$AWS_REGION"
    
    # Configurar también en ~/.aws/credentials
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
    
    # Verificar que funciona
    echo -e "${BLUE}🔍 Verificando credenciales...${NC}"
    if aws sts get-caller-identity &> /dev/null; then
        echo -e "${GREEN}✅ Credenciales de AWS configuradas correctamente${NC}"
        echo "Usuario: $(aws sts get-caller-identity --query 'Arn' --output text)"
        echo "Región: $AWS_REGION"
    else
        echo -e "${RED}❌ Error al verificar credenciales de AWS${NC}"
        echo "Verifica que las credenciales estén correctas y que no hayan expirado"
        exit 1
    fi
}

# Función para generar secrets
generate_secrets() {
    echo -e "${BLUE}🔐 Generando secrets necesarios...${NC}"
    
    # Generar JWT Secret
    JWT_SECRET=$(openssl rand -base64 32)
    echo -e "${GREEN}✅ JWT Secret generado${NC}"
    
    # Generar Database Password
    DATABASE_PASSWORD=$(openssl rand -base64 16 | tr -d '/+=' | cut -c1-16)
    echo -e "${GREEN}✅ Database Password generado${NC}"
    
    # Obtener región actual
    AWS_REGION=$(aws configure get region 2>/dev/null || echo $AWS_DEFAULT_REGION)
    
    # Obtener credenciales actuales
    AWS_ACCESS_KEY_ID=$(aws configure get aws_access_key_id 2>/dev/null || echo $AWS_ACCESS_KEY_ID)
    AWS_SECRET_ACCESS_KEY=$(aws configure get aws_secret_access_key 2>/dev/null || echo $AWS_SECRET_ACCESS_KEY)
    AWS_SESSION_TOKEN=$(aws configure get aws_session_token 2>/dev/null || echo $AWS_SESSION_TOKEN)
    
    echo -e "${GREEN}✅ Secrets generados correctamente${NC}"
}

# Función para configurar Cloudinary
setup_cloudinary() {
    echo -e "${BLUE}☁️  Configurando Cloudinary...${NC}"
    echo ""
    echo "Cloudinary es opcional pero recomendado para subir imágenes."
    echo "Puedes obtener las credenciales en: https://cloudinary.com/console"
    echo ""
    
    read -p "¿Deseas configurar Cloudinary? (y/n): " setup_cloudinary
    if [[ "$setup_cloudinary" == "y" || "$setup_cloudinary" == "Y" ]]; then
        read -p "Cloudinary Cloud Name: " CLOUDINARY_CLOUD_NAME
        read -p "Cloudinary API Key: " CLOUDINARY_API_KEY
        read -p "Cloudinary API Secret: " CLOUDINARY_API_SECRET
        echo -e "${GREEN}✅ Cloudinary configurado${NC}"
    else
        CLOUDINARY_CLOUD_NAME="your_cloud_name"
        CLOUDINARY_API_KEY="your_api_key"
        CLOUDINARY_API_SECRET="your_api_secret"
        echo -e "${YELLOW}⚠️  Cloudinary no configurado (podrás configurarlo después)${NC}"
    fi
}

# Función para mostrar instrucciones de GitHub Secrets
show_github_instructions() {
    echo -e "${BLUE}📋 Configuración de GitHub Secrets${NC}"
    echo ""
    echo "Ahora necesitas configurar los siguientes secrets en tu repositorio de GitHub:"
    echo "Ir a: Repository → Settings → Secrets and variables → Actions → New repository secret"
    echo ""
    echo "╔══════════════════════════════════════════════════════════════════════════════════════╗"
    echo "║                                 GITHUB SECRETS                                      ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════════════╝"
    echo ""
    echo -e "${YELLOW}AWS Configuration:${NC}"
    echo "AWS_ACCESS_KEY_ID: $AWS_ACCESS_KEY_ID"
    echo "AWS_SECRET_ACCESS_KEY: $AWS_SECRET_ACCESS_KEY"
    echo "AWS_SESSION_TOKEN: $AWS_SESSION_TOKEN"
    echo "AWS_REGION: $AWS_REGION"
    echo ""
    echo -e "${YELLOW}Application Configuration:${NC}"
    echo "DATABASE_PASSWORD: $DATABASE_PASSWORD"
    echo "JWT_SECRET: $JWT_SECRET"
    echo "CLOUDINARY_CLOUD_NAME: $CLOUDINARY_CLOUD_NAME"
    echo "CLOUDINARY_API_KEY: $CLOUDINARY_API_KEY"
    echo "CLOUDINARY_API_SECRET: $CLOUDINARY_API_SECRET"
    echo ""
    echo -e "${YELLOW}EC2 Configuration (se configurarán automáticamente):${NC}"
    echo "EC2_HOST: (se configurará automáticamente)"
    echo "EC2_USER: ubuntu"
    echo "EC2_PRIVATE_KEY: (se configurará automáticamente)"
    echo ""
    echo "╔══════════════════════════════════════════════════════════════════════════════════════╗"
    echo "║                              COPIA Y PEGA EN GITHUB                                 ║"
    echo "╚══════════════════════════════════════════════════════════════════════════════════════╝"
    echo ""
    
    # Crear archivo con los secrets
    cat > github-secrets.txt << EOF
# GitHub Secrets para Booky Backend
# Copia estos valores en GitHub → Settings → Secrets and variables → Actions

# AWS Configuration
AWS_ACCESS_KEY_ID=$AWS_ACCESS_KEY_ID
AWS_SECRET_ACCESS_KEY=$AWS_SECRET_ACCESS_KEY
AWS_SESSION_TOKEN=$AWS_SESSION_TOKEN
AWS_REGION=$AWS_REGION

# Application Configuration
DATABASE_PASSWORD=$DATABASE_PASSWORD
JWT_SECRET=$JWT_SECRET
CLOUDINARY_CLOUD_NAME=$CLOUDINARY_CLOUD_NAME
CLOUDINARY_API_KEY=$CLOUDINARY_API_KEY
CLOUDINARY_API_SECRET=$CLOUDINARY_API_SECRET

# EC2 Configuration (se configurarán automáticamente)
# EC2_HOST: (se configurará automáticamente)
# EC2_USER: ubuntu
# EC2_PRIVATE_KEY: (se configurará automáticamente)
EOF
    
    echo -e "${GREEN}✅ Secrets guardados en github-secrets.txt${NC}"
    echo -e "${YELLOW}⚠️  IMPORTANTE: Este archivo NO se subirá a GitHub (está en .gitignore)${NC}"
    echo ""
    echo "Presiona Enter después de configurar los secrets en GitHub..."
    read
}

# Función para crear el archivo .env local
create_local_env() {
    echo -e "${BLUE}📄 Creando archivo .env local...${NC}"
    
    cat > .env << EOF
# Booky Backend - Local Environment
# Este archivo es para desarrollo local

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
    
    echo -e "${GREEN}✅ Archivo .env creado para desarrollo local${NC}"
}

# Función para mostrar próximos pasos
show_next_steps() {
    echo -e "${BLUE}🎯 Próximos pasos:${NC}"
    echo ""
    echo "1. ✅ Configurar los secrets en GitHub (ya te mostramos cómo)"
    echo "2. 🚀 Hacer push a la rama main/master para iniciar el deployment"
    echo "3. 🔍 Monitorear el deployment en GitHub Actions"
    echo "4. 🌐 Acceder a tu aplicación cuando esté desplegada"
    echo ""
    echo -e "${YELLOW}Comandos útiles:${NC}"
    echo "- Ver logs del deployment: Repository → Actions → Deploy to EC2"
    echo "- Conectarse al servidor: ./scripts/manage-server.sh connect"
    echo "- Ver estado: ./scripts/manage-server.sh status"
    echo "- Ver logs: ./scripts/manage-server.sh logs"
    echo ""
    echo -e "${GREEN}🎉 ¡Configuración completada!${NC}"
    echo ""
    echo "Después del primer deployment, encontrarás la IP pública en los logs de GitHub Actions."
    echo "Tu aplicación estará disponible en: http://TU_IP_PUBLICA"
}

# Función para verificar configuración
verify_setup() {
    echo -e "${BLUE}🔍 Verificando configuración...${NC}"
    
    # Verificar que los archivos necesarios existen
    if [[ -f ".github/workflows/deploy.yml" ]]; then
        echo -e "${GREEN}✅ Workflow de GitHub Actions configurado${NC}"
    else
        echo -e "${RED}❌ Workflow de GitHub Actions no encontrado${NC}"
    fi
    
    if [[ -f "scripts/setup-ec2.sh" ]]; then
        echo -e "${GREEN}✅ Script de setup EC2 encontrado${NC}"
    else
        echo -e "${RED}❌ Script de setup EC2 no encontrado${NC}"
    fi
    
    if [[ -f "scripts/deploy.sh" ]]; then
        echo -e "${GREEN}✅ Script de deployment encontrado${NC}"
    else
        echo -e "${RED}❌ Script de deployment no encontrado${NC}"
    fi
    
    if [[ -f "scripts/manage-server.sh" ]]; then
        echo -e "${GREEN}✅ Script de gestión encontrado${NC}"
    else
        echo -e "${RED}❌ Script de gestión no encontrado${NC}"
    fi
    
    # Verificar permisos de scripts
    chmod +x scripts/*.sh
    chmod +x setup-aws-deployment.sh
    
    echo -e "${GREEN}✅ Permisos de scripts configurados${NC}"
}

# Función principal
main() {
    show_banner
    show_help
    check_tools
    setup_aws_credentials
    generate_secrets
    setup_cloudinary
    show_github_instructions
    create_local_env
    verify_setup
    show_next_steps
    
    echo -e "${GREEN}✨ ¡Setup completado exitosamente!${NC}"
    echo ""
    echo "Archivos creados:"
    echo "- .env (para desarrollo local)"
    echo "- github-secrets.txt (para configurar GitHub Secrets)"
    echo ""
    echo "Cuando hayas configurado los secrets en GitHub, haz:"
    echo "git add ."
    echo "git commit -m 'Configure AWS deployment'"
    echo "git push origin main"
    echo ""
    echo "¡El deployment se iniciará automáticamente!"
}

# Ejecutar función principal
main "$@" 