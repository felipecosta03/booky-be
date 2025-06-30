#!/bin/bash

# 🔄 Booky Backend - Restart Only Backend App
# Rebuilda y reinicia solo el backend, mantiene DB corriendo

echo "🔄 Restarting Booky Backend App..."
echo "This will rebuild and restart only the backend application"
echo "Database and Adminer will keep running"
echo ""

# Load environment variables from .env file
if [ -f .env ]; then
    echo "📋 Loading environment variables from .env file..."
    export $(cat .env | grep -v '#' | grep -v '^$' | xargs)
else
    echo "⚠️  Warning: .env file not found, using default values"
fi

# Stop and remove backend container
echo "🛑 Stopping backend container..."
docker stop booky-backend 2>/dev/null || echo "Backend not running"
docker rm booky-backend 2>/dev/null || echo "Backend container not found"

# Remove old backend image
echo "🗑️  Removing old backend image..."
docker rmi booky-backend:latest 2>/dev/null || echo "No old image to remove"

# Build new backend image
echo "🔨 Building new backend image..."
docker build -t booky-backend .

# Start backend application with environment variables from .env
echo "🚀 Starting backend application..."
docker run -d \
  --name booky-backend \
  --network booky-network \
  -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://booky-postgres:5432/${DATABASE_NAME:-booky} \
  -e DATABASE_USERNAME=${DATABASE_USERNAME:-postgres} \
  -e DATABASE_PASSWORD=${DATABASE_PASSWORD:-admin} \
  -e DATABASE_NAME=${DATABASE_NAME:-booky} \
  -e DDL_AUTO=${DDL_AUTO:-update} \
  -e CLOUDINARY_CLOUD_NAME=${CLOUDINARY_CLOUD_NAME:-} \
  -e CLOUDINARY_API_KEY=${CLOUDINARY_API_KEY:-} \
  -e CLOUDINARY_API_SECRET=${CLOUDINARY_API_SECRET:-} \
  -e JWT_SECRET=${JWT_SECRET:-your-super-secret-jwt-key-minimum-32-characters-for-development} \
  -e JWT_EXPIRATION=${JWT_EXPIRATION:-86400000} \
  -e LOG_LEVEL=${LOG_LEVEL:-INFO} \
  -e APP_LOG_LEVEL=${APP_LOG_LEVEL:-DEBUG} \
  -e SECURITY_LOG_LEVEL=${SECURITY_LOG_LEVEL:-INFO} \
  -e SHOW_SQL=${SHOW_SQL:-false} \
  -e FORMAT_SQL=${FORMAT_SQL:-true} \
  -e CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS:-http://localhost:3000,http://localhost:4200} \
  -e CORS_ALLOWED_METHODS=${CORS_ALLOWED_METHODS:-GET,POST,PUT,DELETE,OPTIONS} \
  -e "CORS_ALLOWED_HEADERS=${CORS_ALLOWED_HEADERS:-*}" \
  -e CORS_ALLOW_CREDENTIALS=${CORS_ALLOW_CREDENTIALS:-true} \
  -e SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-docker} \
  -e OPENAPI_DEV_URL=${OPENAPI_DEV_URL:-http://localhost:8080} \
  -e OPENAPI_PROD_URL=${OPENAPI_PROD_URL:-https://your-production-url.com} \
  booky-backend:latest

echo ""
echo "✅ Backend restarted successfully!"
echo ""
echo "📋 Application URL:"
echo "   🚀 API:          http://localhost:8080"
echo "   📚 Swagger UI:   http://localhost:8080/swagger-ui/index.html"
echo ""
echo "🧪 Test endpoints:"
echo "   curl \"http://localhost:8080/books/search?q=hobbit\""
echo "   curl \"http://localhost:8080/users/user-001\""
echo ""
echo "📜 To view logs: docker logs booky-backend -f" 