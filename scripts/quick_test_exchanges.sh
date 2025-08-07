#!/bin/bash

# Script de testing rápido para exchanges
# Uso: ./quick_test_exchanges.sh [BASE_URL]

BASE_URL=${1:-"http://localhost:8080"}

echo "🚀 Testing rápido de BookExchange endpoints"
echo "Base URL: $BASE_URL"
echo "=========================================="

# Variables de testing (usuarios reales después de ejecutar alta_usuarios.sql)
USER_REQUESTER="user-001"  # Juan Pérez
USER_OWNER="user-002"      # María García

echo "👥 Usuarios de prueba (datos reales):"
echo "   Solicitante: $USER_REQUESTER (Juan Pérez)"  
echo "   Propietario: $USER_OWNER (María García)"
echo ""

# 1. Crear intercambio con userbooks reales
echo "📝 1. Creando intercambio con IDs reales de userbooks..."
echo "   Juan ofrece: El Gran Gatsby + Cien Años de Soledad"
echo "   María tiene: Matar un Ruiseñor + El Retrato de Dorian Gray"
EXCHANGE_RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d "{
    \"requesterId\": \"$USER_REQUESTER\",
    \"ownerId\": \"$USER_OWNER\",
    \"ownerBookIds\": [\"ub-007\", \"ub-010\"],
    \"requesterBookIds\": [\"ub-002\", \"ub-005\"]
  }" \
  "$BASE_URL/exchanges")

echo "$EXCHANGE_RESPONSE" | jq '.' 2>/dev/null || echo "$EXCHANGE_RESPONSE"

# Extraer ID del exchange
EXCHANGE_ID=$(echo "$EXCHANGE_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -n "$EXCHANGE_ID" ]; then
    echo "✅ Exchange creado con ID: $EXCHANGE_ID"
else
    echo "❌ Error creando exchange"
    exit 1
fi

echo ""

# 2. Obtener intercambios del solicitante
echo "🔍 2. Intercambios del solicitante:"
curl -s -X GET \
  -H "Accept: application/json" \
  "$BASE_URL/exchanges/users/$USER_REQUESTER" | jq '.' 2>/dev/null || echo "Error en request"

echo ""

# 3. Obtener intercambios del propietario
echo "🔍 3. Intercambios del propietario:"
curl -s -X GET \
  -H "Accept: application/json" \
  "$BASE_URL/exchanges/users/$USER_OWNER" | jq '.' 2>/dev/null || echo "Error en request"

echo ""

# 4. Obtener intercambio específico
echo "🔍 4. Intercambio específico ($EXCHANGE_ID):"
curl -s -X GET \
  -H "Accept: application/json" \
  "$BASE_URL/exchanges/$EXCHANGE_ID" | jq '.' 2>/dev/null || echo "Error en request"

echo ""

# 5. Aceptar intercambio
echo "✅ 5. Aceptando intercambio (como propietario):"
curl -s -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{"status": "ACCEPTED"}' \
  "$BASE_URL/exchanges/$EXCHANGE_ID/status?userId=$USER_OWNER" | jq '.' 2>/dev/null || echo "Error en request"

echo ""

# 6. Contar pendientes
echo "📊 6. Intercambios pendientes para el solicitante:"
curl -s -X GET \
  -H "Accept: application/json" \
  "$BASE_URL/exchanges/users/$USER_REQUESTER/pending-count"

echo ""
echo ""

# 7. Crear contraoferta
echo "🔄 7. Creando contraoferta (María cambia la oferta):"
echo "   Nueva oferta: Solo Matar un Ruiseñor por solo Cien Años de Soledad"
curl -s -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "ownerBookIds": ["ub-007"],
    "requesterBookIds": ["ub-005"]
  }' \
  "$BASE_URL/exchanges/$EXCHANGE_ID/counter-offer?userId=$USER_OWNER" | jq '.' 2>/dev/null || echo "Error en request"

echo ""

# 8. Filtrar por status
echo "🔍 8. Intercambios COUNTERED del propietario:"
curl -s -X GET \
  -H "Accept: application/json" \
  "$BASE_URL/exchanges/users/$USER_OWNER?status=COUNTERED" | jq '.' 2>/dev/null || echo "Error en request"

echo ""
echo "🎉 Testing completado!"
echo ""
echo "📋 Comandos útiles adicionales:"
echo "   Ver libros para intercambio: curl '$BASE_URL/books/exchange'"
echo "   Ver biblioteca de Juan: curl '$BASE_URL/books/users/$USER_REQUESTER/library'"
echo "   Ver biblioteca de María: curl '$BASE_URL/books/users/$USER_OWNER/library'"
echo "   Completar exchange: curl -X PUT -H 'Content-Type: application/json' -d '{\"status\":\"COMPLETED\"}' '$BASE_URL/exchanges/$EXCHANGE_ID/status?userId=$USER_OWNER'"
echo ""
echo "💡 NOTA: Para usar este script, primero ejecuta:"
echo "   1. scripts/alta_usuarios.sql"
echo "   2. scripts/alta_libros_userbooks.sql"