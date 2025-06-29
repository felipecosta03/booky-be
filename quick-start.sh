#!/bin/bash

# 🚀 Booky Backend - Quick Start
# Ejecuta la aplicación directamente sin menús

echo "🚀 Starting Booky Backend..."
echo "Building and starting containers..."

# Start the application
docker-compose up --build -d

echo ""
echo "✅ Application started!"
echo ""
echo "📋 Available services:"
echo "   🚀 API:          http://localhost:8080"
echo "   📚 Swagger UI:   http://localhost:8080/swagger-ui/index.html"
echo "   🗄️  Adminer:      http://localhost:8081"
echo "   📊 Database:     localhost:5433 (postgres/admin)"
echo ""
echo "🧪 Test endpoints:"
echo "   curl http://localhost:8080/api/books/search?q=test"
echo "   curl -X POST http://localhost:8080/api/books/users/user-001/library \\"
echo "        -H 'Content-Type: application/json' \\"
echo "        -d '{\"isbn\":\"9780547928227\",\"status\":\"TO_READ\"}'"
echo ""
echo "📜 To view logs: docker-compose logs -f"
echo "🛑 To stop: docker-compose down" 