#!/bin/bash

# ==========================================
# Deploy App Only - Booky Backend
# ==========================================
# Script para desplegar solo la aplicación sin tocar PostgreSQL

set -e

echo "🚀 Starting application-only deployment..."

# Verificar si estamos en el directorio correcto
if [ ! -f "docker-compose-ec2.yml" ]; then
    echo "❌ Error: docker-compose-ec2.yml not found. Run from project root."
    exit 1
fi

# Actualizar código fuente
echo "📥 Pulling latest changes..."
git pull origin master

# Construir nueva imagen
echo "🏗️  Building Docker image..."
docker build -t booky-backend:latest .

# Verificar si PostgreSQL está corriendo
echo "🔍 Checking PostgreSQL status..."
if ! docker ps | grep -q booky-postgres; then
    echo "🐘 PostgreSQL not running, starting database services..."
    docker-compose -f docker-compose-ec2.yml up -d postgres adminer
    echo "⏳ Waiting for PostgreSQL to be ready..."
    sleep 30
else
    echo "✅ PostgreSQL is already running"
fi

# Parar y eliminar contenedor de aplicación actual
echo "🛑 Stopping current application container..."
docker stop booky-backend || true
docker rm booky-backend || true

# Ejecutar nuevo contenedor
echo "🚀 Starting new application container..."
docker run -d \
  --name booky-backend \
  --network booky-be_booky-network \
  -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://postgres:5432/booky \
  -e DATABASE_USERNAME=postgres \
  -e DATABASE_PASSWORD=admin \
  -e DATABASE_NAME=booky \
  -e SPRING_PROFILES_ACTIVE=local \
  -e JWT_SECRET=booky-super-secret-jwt-key-for-development-only-32-chars \
  -e JWT_EXPIRATION=86400000 \
  -e SHOW_SQL=true \
  -e FORMAT_SQL=true \
  booky-backend:latest

# Verificar despliegue
echo "⏳ Waiting for application to start..."
sleep 15

echo "🔍 Checking application health..."
for i in {1..30}; do
  if curl -f http://localhost:8080/actuator/health 2>/dev/null; then
    echo "✅ Application is healthy!"
    break
  fi
  echo "Attempt $i: Application not ready, waiting..."
  sleep 5
done

# Limpiar imágenes viejas
echo "🧹 Cleaning up old Docker images..."
docker image prune -f

# Mostrar resumen
echo ""
echo "=== DEPLOYMENT SUMMARY ==="
echo "✅ Application updated successfully"
echo "🐘 PostgreSQL: $(docker ps | grep booky-postgres > /dev/null && echo "Running" || echo "Not running")"
echo "🚀 Application: $(docker ps | grep booky-backend > /dev/null && echo "Running" || echo "Not running")"
echo "📊 Adminer: $(docker ps | grep booky-adminer > /dev/null && echo "Running" || echo "Not running")"
echo "🌐 App URL: http://52.15.181.167:8080"
echo "🔧 Adminer URL: http://52.15.181.167:8081"
echo "=========================="

echo "🎉 Deployment completed successfully!" 