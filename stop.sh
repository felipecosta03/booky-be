#!/bin/bash

# 🛑 Booky Backend - Stop All Services
# Para y elimina todos los contenedores y recursos

echo "🛑 Stopping Booky Backend Application..."
echo "This will stop and remove all containers"
echo ""

# Stop containers
echo "🛑 Stopping containers..."
docker stop booky-backend booky-postgres booky-adminer 2>/dev/null || echo "Some containers were not running"

# Remove containers
echo "🗑️  Removing containers..."
docker rm booky-backend booky-postgres booky-adminer 2>/dev/null || echo "Some containers were not found"

# Remove network
echo "🗑️  Removing network..."
docker network rm booky-network 2>/dev/null || echo "Network not found"

# Optional: Remove volumes (uncomment if you want to delete database data)
# echo "🗑️  Removing volumes..."
# docker volume rm postgres_data 2>/dev/null || echo "Volume not found"

# Optional: Remove images (uncomment if you want to delete images)
# echo "🗑️  Removing images..."
# docker rmi booky-backend:latest postgres:15-alpine adminer:4.8.1 2>/dev/null || echo "Some images not found"

echo ""
echo "✅ All services stopped successfully!"
echo ""
echo "💡 Notes:"
echo "   📊 Database data preserved in volume 'postgres_data'"
echo "   🔨 Backend image preserved for faster restart"
echo ""
echo "🚀 To start again: ./start.sh"
echo "🧹 To remove everything (including data): docker system prune -a --volumes" 