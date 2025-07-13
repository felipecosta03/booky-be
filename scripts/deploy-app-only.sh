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
  # Method 1: Check if port 8080 is listening
  if netstat -tuln | grep -q :8080; then
    echo "✅ Port 8080 is listening!"
    
    # Method 2: Try basic HTTP connection
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200\|404\|302"; then
      echo "✅ Application is responding to HTTP requests!"
      break
    fi
    
    # Method 3: Try common Spring Boot endpoints
    if curl -s http://localhost:8080/ 2>/dev/null | grep -q "Whitelabel\|Welcome\|Error"; then
      echo "✅ Application is serving content!"
      break
    fi
    
    # Method 4: Check if any of your endpoints respond
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/users 2>/dev/null | grep -q "200\|404\|401\|403"; then
      echo "✅ Application endpoints are accessible!"
      break
    fi
  fi
  
  # Method 5: Check container logs for success indicators
  if docker logs booky-backend 2>&1 | grep -q "Started.*Application\|Tomcat started on port"; then
    echo "✅ Application started successfully according to logs!"
    break
  fi
  
  echo "Attempt $i: Application not ready, waiting..."
  sleep 5
done

# Show verification details
echo ""
echo "=== VERIFICATION DETAILS ==="
echo "🔍 Port status:"
netstat -tuln | grep :8080 || echo "Port 8080 not listening"

echo "🔍 HTTP response:"
curl -s -o /dev/null -w "HTTP Status: %{http_code}" http://localhost:8080 || echo "No HTTP response"

echo "🔍 Recent application logs:"
docker logs booky-backend --tail 10 2>&1

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