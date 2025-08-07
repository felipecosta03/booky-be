# Comandos cURL para Testing de Endpoints de BookExchange

## Configuración
```bash
BASE_URL="http://localhost:8080"
USER_ID_1="user-requester-123"
USER_ID_2="user-owner-456"
EXCHANGE_ID="exchange-abc123"
```

## 📋 Tabla de Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/exchanges` | Crear nuevo intercambio |
| GET | `/exchanges/users/{userId}` | Obtener intercambios de usuario |
| GET | `/exchanges/users/{userId}/as-requester` | Intercambios como solicitante |
| GET | `/exchanges/users/{userId}/as-owner` | Intercambios como propietario |
| GET | `/exchanges/{exchangeId}` | Obtener intercambio por ID |
| PUT | `/exchanges/{exchangeId}/status` | Actualizar estado del intercambio |
| PUT | `/exchanges/{exchangeId}/counter-offer` | Crear contraoferta |
| GET | `/exchanges/users/{userId}/pending-count` | Contar intercambios pendientes |

---

## 🔨 1. Crear un nuevo intercambio

### Request
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "requesterId": "user-requester-123",
    "ownerId": "user-owner-456",
    "ownerBookIds": ["book-1", "book-2"],
    "requesterBookIds": ["book-3", "book-4"]
  }' \
  "http://localhost:8080/exchanges"
```

### Response esperado (201 Created)
```json
{
  "id": "exchange-abc123",
  "requesterId": "user-requester-123",
  "ownerId": "user-owner-456",
  "status": "PENDING",
  "dateCreated": "2024-01-15T10:30:00",
  "dateUpdated": "2024-01-15T10:30:00",
  "ownerBookIds": ["book-1", "book-2"],
  "requesterBookIds": ["book-3", "book-4"]
}
```

---

## 📖 2. Obtener todos los intercambios de un usuario

### Request (sin filtro de status)
```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-requester-123"
```

### Request (con filtro por status)
```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-requester-123?status=PENDING"
```

### Valores válidos para status:
- `PENDING` - Intercambio pendiente
- `ACCEPTED` - Intercambio aceptado
- `REJECTED` - Intercambio rechazado
- `COUNTERED` - Contraoferta realizada
- `CANCELLED` - Intercambio cancelado
- `COMPLETED` - Intercambio completado

---

## 🙋‍♂️ 3. Obtener intercambios donde el usuario es solicitante

```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-requester-123/as-requester"
```

---

## 👤 4. Obtener intercambios donde el usuario es propietario

```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-owner-456/as-owner"
```

---

## 🔍 5. Obtener intercambio específico por ID

```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/exchange-abc123"
```

---

## ✅ 6. Actualizar estado del intercambio

### Aceptar intercambio (por el propietario)
```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "status": "ACCEPTED"
  }' \
  "http://localhost:8080/exchanges/exchange-abc123/status?userId=user-owner-456"
```

### Rechazar intercambio (por el propietario)
```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "status": "REJECTED"
  }' \
  "http://localhost:8080/exchanges/exchange-abc123/status?userId=user-owner-456"
```

### Cancelar intercambio (por el solicitante)
```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "status": "CANCELLED"
  }' \
  "http://localhost:8080/exchanges/exchange-abc123/status?userId=user-requester-123"
```

### Completar intercambio
```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "status": "COMPLETED"
  }' \
  "http://localhost:8080/exchanges/exchange-abc123/status?userId=user-owner-456"
```

---

## 🔄 7. Crear contraoferta

```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "ownerBookIds": ["book-5", "book-6"],
    "requesterBookIds": ["book-7"]
  }' \
  "http://localhost:8080/exchanges/exchange-abc123/counter-offer?userId=user-owner-456"
```

---

## 📊 8. Obtener cantidad de intercambios pendientes

```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-requester-123/pending-count"
```

### Response esperado
```json
3
```

---

## 🧪 Script de Testing Completo

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"
USER_ID_1="user-requester-123"
USER_ID_2="user-owner-456"

echo "=== Testing BookExchange Endpoints ==="

echo "1. Creando intercambio..."
EXCHANGE_RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "'$USER_ID_1'",
    "ownerId": "'$USER_ID_2'",
    "ownerBookIds": ["book-1", "book-2"],
    "requesterBookIds": ["book-3", "book-4"]
  }' \
  "$BASE_URL/exchanges")

EXCHANGE_ID=$(echo $EXCHANGE_RESPONSE | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "Exchange ID: $EXCHANGE_ID"

echo -e "\n2. Obteniendo intercambios del usuario solicitante..."
curl -s -X GET "$BASE_URL/exchanges/users/$USER_ID_1" | jq '.'

echo -e "\n3. Obteniendo intercambios del usuario propietario..."
curl -s -X GET "$BASE_URL/exchanges/users/$USER_ID_2" | jq '.'

echo -e "\n4. Actualizando estado a ACCEPTED..."
curl -s -X PUT \
  -H "Content-Type: application/json" \
  -d '{"status": "ACCEPTED"}' \
  "$BASE_URL/exchanges/$EXCHANGE_ID/status?userId=$USER_ID_2" | jq '.'

echo -e "\n5. Obteniendo count de pendientes..."
curl -s -X GET "$BASE_URL/exchanges/users/$USER_ID_1/pending-count"

echo -e "\n=== Testing Completado ==="
```

---

## 📝 Notas Importantes

1. **Autenticación**: Los endpoints actualmente no requieren autenticación según la configuración (`security.enabled: false`)

2. **Validaciones**: 
   - El solicitante y propietario deben ser diferentes
   - Debe haber al menos un libro en cada lista
   - Los libros deben existir y pertenecer a los usuarios correspondientes

3. **Estados de Intercambio**:
   - Solo el propietario puede ACEPTAR/RECHAZAR/hacer COUNTER-OFFER
   - Solo el solicitante puede CANCELAR
   - Ambos pueden marcar como COMPLETED

4. **Formato de Response**: Todos los responses de éxito retornan el objeto `BookExchangeDto` completo

5. **Códigos de Error Comunes**:
   - `400`: Datos de request inválidos
   - `403`: Usuario no autorizado para la acción
   - `404`: Intercambio no encontrado
   - `409`: No se puede crear intercambio consigo mismo