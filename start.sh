#!/bin/bash

# 🚀 Booky Backend - Start Complete Application
# Levanta toda la aplicación: DB + Backend + Adminer

echo "🚀 Starting Booky Backend Complete Application..."

# Load environment variables from .env file
if [ -f .env ]; then
    echo "📋 Loading environment variables from .env file..."
    export $(cat .env | grep -v '#' | grep -v '^$' | xargs)
else
    echo "⚠️  Warning: .env file not found, using default values"
fi

# Create network if it doesn't exist
echo "📡 Creating network..."
docker network create booky-network 2>/dev/null || echo "Network already exists"

# Start PostgreSQL
echo "🗄️  Starting PostgreSQL..."
docker run -d \
  --name booky-postgres \
  --network booky-network \
  -e POSTGRES_DB=${DATABASE_NAME:-booky} \
  -e POSTGRES_USER=${DATABASE_USERNAME:-postgres} \
  -e POSTGRES_PASSWORD=${DATABASE_PASSWORD:-admin} \
  -e PGDATA=/var/lib/postgresql/data/pgdata \
  -p 5433:5432 \
  -v postgres_data:/var/lib/postgresql/data \
  -v "$(pwd)/database_schema_updated.sql:/docker-entrypoint-initdb.d/00-schema.sql" \
  -v "$(pwd)/scripts/alta_usuarios.sql:/docker-entrypoint-initdb.d/01-alta_usuarios.sql" \
  postgres:15-alpine 2>/dev/null || echo "PostgreSQL already running"

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 5

# Build backend image
echo "🔨 Building backend application..."
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
  booky-backend:latest 2>/dev/null || echo "Backend already running"

# Start Adminer
echo "🗄️  Starting Adminer..."
docker run -d \
  --name booky-adminer \
  --network booky-network \
  -p 8081:8080 \
  adminer:4.8.1 2>/dev/null || echo "Adminer already running"

echo ""
echo "✅ Application started successfully!"
echo ""
echo "📋 Available services:"
echo "   🚀 API:          http://localhost:8080"
echo "   📚 Swagger UI:   http://localhost:8080/swagger-ui/index.html"
echo "   🗄️  Adminer:      http://localhost:8081"
echo "   📊 Database:     localhost:5433 (${DATABASE_USERNAME:-postgres}/${DATABASE_PASSWORD:-admin})"
echo ""
echo "🧪 Test endpoints:"
echo "   curl \"http://localhost:8080/books/search?q=hobbit\""
echo "   curl \"http://localhost:8080/books/isbn/9780547928227\""
echo "   curl \"http://localhost:8080/users/user-001\""
echo ""
echo "📜 To view logs: docker logs booky-backend -f"
echo "🛑 To stop all:  ./stop.sh" 