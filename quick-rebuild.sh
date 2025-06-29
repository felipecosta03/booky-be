#!/bin/bash

# 🔨 Booky Backend - Quick Rebuild
# Rebuild la aplicación desde cero eliminando todo

echo "🔨 Rebuilding Booky Backend from scratch..."
echo "This will:"
echo "  - Stop all containers"
echo "  - Remove containers, images, and volumes"
echo "  - Prune Docker system"
echo "  - Build and start fresh"
echo ""

read -p "Are you sure? (y/N): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Rebuild cancelled."
    exit 1
fi

echo "🛑 Stopping and removing everything..."
docker-compose down -v --rmi all --remove-orphans

echo "🧹 Pruning Docker system..."
docker system prune -f

echo "🔨 Building and starting fresh containers..."
docker-compose up --build -d

echo ""
echo "✅ Application rebuilt from scratch!"
echo ""
echo "📋 Available services:"
echo "   🚀 API:          http://localhost:8080"
echo "   📚 Swagger UI:   http://localhost:8080/swagger-ui/index.html"
echo "   🗄️  Adminer:      http://localhost:8081"
echo "   📊 Database:     localhost:5433 (postgres/admin)"
echo ""
echo "📜 To view logs: docker-compose logs -f"
echo "🛑 To stop: docker-compose down" 