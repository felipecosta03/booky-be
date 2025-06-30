#!/bin/bash

# 📊 Booky Backend - Status Check
# Muestra el estado de todos los servicios y logs recientes

echo "📊 Booky Backend - Application Status"
echo "======================================"
echo ""

# Check container status
echo "🔍 Container Status:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=booky"
echo ""

# Check if services are responding
echo "🌐 Service Health Check:"

# Check backend
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health 2>/dev/null | grep -q "200"; then
    echo "   ✅ Backend API (8080): HEALTHY"
else
    echo "   ❌ Backend API (8080): NOT RESPONDING"
fi

# Check Swagger UI
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui/index.html 2>/dev/null | grep -q "401\|200"; then
    echo "   ✅ Swagger UI (8080): ACCESSIBLE"
else
    echo "   ❌ Swagger UI (8080): NOT ACCESSIBLE"
fi

# Check Adminer
if curl -s -o /dev/null -w "%{http_code}" http://localhost:8081 2>/dev/null | grep -q "200"; then
    echo "   ✅ Adminer (8081): ACCESSIBLE"
else
    echo "   ❌ Adminer (8081): NOT ACCESSIBLE"
fi

echo ""

# Show recent logs
echo "📜 Recent Backend Logs (last 10 lines):"
echo "----------------------------------------"
docker logs booky-backend --tail 10 2>/dev/null || echo "Backend container not found or not running"
echo ""

# Test endpoints
echo "🧪 Quick API Test:"
echo "-------------------"
echo "Testing books search endpoint..."
if curl -s "http://localhost:8080/books/search?q=test" | head -5 >/dev/null 2>&1; then
    echo "   ✅ Books search endpoint: OK"
else
    echo "   ❌ Books search endpoint: FAILED"
fi

echo ""
echo "📋 Quick Commands:"
echo "   View full logs:    docker logs booky-backend -f"
echo "   Restart backend:   ./restart-app.sh"
echo "   Stop all:          ./stop.sh" 