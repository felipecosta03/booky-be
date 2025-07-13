#!/bin/bash

# ==========================================
# BOOKY BACKEND - VERIFICATION SCRIPT
# ==========================================
# Script para verificar que todos los archivos necesarios estén disponibles

echo "🔍 Verificando configuración de Booky Backend Free Tier..."

# Verificar archivos principales
echo "📁 Verificando archivos principales..."

files_required=(
    "scripts/free-tier-deploy.sh"
    "scripts/manage-free-tier.sh"  
    "scripts/init-database-rds.sql"
    "docker-compose.free-tier.yml"
    "env.free-tier.example"
    "README-FREE-TIER.md"
    "target/booky-backend-*.jar"
)

files_missing=()

for file in "${files_required[@]}"; do
    if [[ "$file" == *"*"* ]]; then
        # Archivo con wildcard
        if ! ls $file &> /dev/null; then
            files_missing+=("$file")
        fi
    else
        # Archivo específico
        if [ ! -f "$file" ]; then
            files_missing+=("$file")
        fi
    fi
done

if [ ${#files_missing[@]} -eq 0 ]; then
    echo "✅ Todos los archivos principales están disponibles"
else
    echo "❌ Archivos faltantes:"
    printf ' - %s\n' "${files_missing[@]}"
    exit 1
fi

# Verificar permisos de ejecución
echo "🔐 Verificando permisos de ejecución..."

if [ -x "scripts/free-tier-deploy.sh" ] && [ -x "scripts/manage-free-tier.sh" ]; then
    echo "✅ Permisos de ejecución correctos"
else
    echo "⚠️  Corrigiendo permisos de ejecución..."
    chmod +x scripts/free-tier-deploy.sh scripts/manage-free-tier.sh
    echo "✅ Permisos corregidos"
fi

# Verificar AWS CLI
echo "🌐 Verificando AWS CLI..."

if command -v aws &> /dev/null; then
    echo "✅ AWS CLI está instalado"
    
    # Verificar credenciales
    if aws sts get-caller-identity &> /dev/null; then
        USER_ARN=$(aws sts get-caller-identity --query 'Arn' --output text)
        echo "✅ Credenciales AWS configuradas: $USER_ARN"
        
        # Verificar que no es AWS Sandbox
        if [[ "$USER_ARN" == *"voclabs"* || "$USER_ARN" == *"student"* || "$USER_ARN" == *"learner"* ]]; then
            echo "⚠️  AWS Sandbox detectado - usa scripts específicos para sandbox"
        else
            echo "✅ Cuenta AWS real detectada - listo para Free Tier"
        fi
    else
        echo "⚠️  Credenciales AWS no configuradas"
        echo "   Ejecuta: aws configure"
    fi
else
    echo "⚠️  AWS CLI no está instalado"
    echo "   Instala desde: https://aws.amazon.com/cli/"
fi

# Verificar Maven y JAR
echo "📦 Verificando aplicación..."

if command -v mvn &> /dev/null; then
    echo "✅ Maven está instalado"
    
    if ls target/booky-backend-*.jar &> /dev/null; then
        echo "✅ Aplicación compilada encontrada"
    else
        echo "⚠️  Aplicación no compilada"
        echo "   Ejecuta: mvn clean package -DskipTests"
    fi
else
    echo "⚠️  Maven no está instalado"
fi

# Verificar Docker (opcional)
echo "🐳 Verificando Docker..."

if command -v docker &> /dev/null; then
    echo "✅ Docker está instalado"
    
    if docker info &> /dev/null; then
        echo "✅ Docker daemon está ejecutándose"
    else
        echo "⚠️  Docker daemon no está ejecutándose"
    fi
else
    echo "ℹ️  Docker no está instalado (opcional para desarrollo local)"
fi

# Resumen final
echo ""
echo "🎯 RESUMEN DE VERIFICACIÓN:"
echo "=========================="

if [ ${#files_missing[@]} -eq 0 ]; then
    echo "✅ Archivos: Todos los archivos necesarios están disponibles"
else
    echo "❌ Archivos: Faltan archivos necesarios"
fi

if command -v aws &> /dev/null && aws sts get-caller-identity &> /dev/null; then
    echo "✅ AWS: Configurado correctamente"
else
    echo "⚠️  AWS: Requiere configuración"
fi

if ls target/booky-backend-*.jar &> /dev/null; then
    echo "✅ Aplicación: Compilada y lista"
else
    echo "⚠️  Aplicación: Requiere compilación"
fi

echo ""
echo "📋 PRÓXIMOS PASOS:"
echo "=================="

if [ ${#files_missing[@]} -eq 0 ]; then
    if command -v aws &> /dev/null && aws sts get-caller-identity &> /dev/null; then
        if ls target/booky-backend-*.jar &> /dev/null; then
            echo "🚀 ¡Todo listo! Puedes ejecutar el deployment:"
            echo "   ./scripts/free-tier-deploy.sh"
        else
            echo "1. Compila la aplicación:"
            echo "   mvn clean package -DskipTests"
            echo "2. Ejecuta el deployment:"
            echo "   ./scripts/free-tier-deploy.sh"
        fi
    else
        echo "1. Configura AWS CLI:"
        echo "   aws configure"
        echo "2. Compila la aplicación (si es necesario):"
        echo "   mvn clean package -DskipTests"
        echo "3. Ejecuta el deployment:"
        echo "   ./scripts/free-tier-deploy.sh"
    fi
else
    echo "❌ Faltan archivos necesarios. Por favor, verifica tu configuración."
fi

echo ""
echo "📚 DOCUMENTACIÓN:"
echo "   - README-FREE-TIER.md - Guía completa"
echo "   - ./scripts/manage-free-tier.sh - Script de gestión"
echo ""
echo "🎉 ¡Verificación completada!" 