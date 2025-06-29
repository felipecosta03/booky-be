#!/bin/bash

# 🔨 Booky Backend - Quick App Rebuild
# Rebuilda solo la aplicación, mantiene BD y otros servicios

echo "🔨 Rebuilding only Booky App..."
echo "This will:"
echo "  - Stop booky-app container"
echo "  - Rebuild booky-app image"
echo "  - Start booky-app container"
echo "  - Keep database and other services running"
echo ""

echo "🛑 Stopping booky-app container..."
docker-compose stop booky-app

echo "🗑️  Removing booky-app container..."
docker-compose rm -f booky-app

echo "🔨 Rebuilding booky-app image..."
docker-compose build --no-cache booky-app

echo "🚀 Starting booky-app container..."
docker-compose up -d booky-app

echo ""
echo "✅ Booky App rebuilt and restarted!"
echo ""
echo "📋 Application URL:"
echo "   🚀 API:          http://localhost:8080"
echo "   📚 Swagger UI:   http://localhost:8080/swagger-ui/index.html"
echo ""
echo "📜 To view logs: docker-compose logs -f booky-app"
echo "📊 To check status: docker-compose ps" 