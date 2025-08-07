# Comandos cURL para Testing de Endpoints de BookExchange
## (ACTUALIZADO con IDs reales de UserBooks)

## Configuración
```bash
BASE_URL="http://localhost:8080"
# Usuarios disponibles (después de ejecutar alta_usuarios.sql y alta_libros_userbooks.sql)
USER_REQUESTER="user-001"  # Juan Pérez
USER_OWNER="user-002"      # María García
```

## 📋 Preparación - IDs de UserBooks Disponibles para Intercambio

### Usuario 1 (user-001 - Juan Pérez):
- `ub-002` - El Gran Gatsby (READ, wants_to_exchange=true)
- `ub-005` - Cien Años de Soledad (READ, wants_to_exchange=true)

### Usuario 2 (user-002 - María García):
- `ub-007` - Matar un Ruiseñor (READ, wants_to_exchange=true)
- `ub-010` - El Retrato de Dorian Gray (READ, wants_to_exchange=true)

### Usuario 3 (user-003 - Carlos Rodríguez):
- `ub-012` - Un Mundo Feliz (READ, wants_to_exchange=true)
- `ub-013` - Harry Potter y la Piedra Filosofal (READ, wants_to_exchange=true)

### Usuario 4 (user-004 - Ana López):
- `ub-017` - Harry Potter y la Piedra Filosofal (READ, wants_to_exchange=true)
- `ub-018` - La Ladrona de Libros (READ, wants_to_exchange=true)

### Usuario 5 (user-005 - Luis Martínez):
- `ub-022` - Crimen y Castigo (READ, wants_to_exchange=true)
- `ub-025` - El Gran Gatsby (READ, wants_to_exchange=true)

---

## 🔨 1. Crear un nuevo intercambio (Ejemplo Realista)

### Juan quiere intercambiar sus libros por los de María
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "requesterId": "user-001",
    "ownerId": "user-002",
    "ownerBookIds": ["ub-007", "ub-010"],
    "requesterBookIds": ["ub-002", "ub-005"]
  }' \
  "http://localhost:8080/exchanges"
```

**Descripción del intercambio:**
- **Juan (requester)** ofrece: "El Gran Gatsby" + "Cien Años de Soledad"
- **María (owner)** recibiría: "Matar un Ruiseñor" + "El Retrato de Dorian Gray"

---

## 📖 2. Obtener todos los intercambios de un usuario

### Intercambios de Juan (como requester y owner)
```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-001"
```

### Filtrar por status PENDING
```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-001?status=PENDING"
```

---

## 🙋‍♂️ 3. Obtener intercambios donde Juan es solicitante

```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-001/as-requester"
```

---

## 👤 4. Obtener intercambios donde María es propietaria

```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/exchanges/users/user-002/as-owner"
```

---

## ✅ 5. María acepta el intercambio

**Paso 1:** Primero crear el intercambio y obtener el ID
```bash
EXCHANGE_RESPONSE=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "user-001",
    "ownerId": "user-002", 
    "ownerBookIds": ["ub-007", "ub-010"],
    "requesterBookIds": ["ub-002", "ub-005"]
  }' \
  "http://localhost:8080/exchanges")

echo $EXCHANGE_RESPONSE
```

**Paso 2:** Extraer el ID y aceptar (reemplazar EXCHANGE_ID con el ID real)
```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "status": "ACCEPTED"
  }' \
  "http://localhost:8080/exchanges/EXCHANGE_ID/status?userId=user-002"
```

---

## 🔄 6. Crear contraoferta

### María hace una contraoferta cambiando los libros
```bash
curl -X PUT \
  -H "Content-Type: application/json" \
  -H "Accept: application/json" \
  -d '{
    "ownerBookIds": ["ub-007"],
    "requesterBookIds": ["ub-005"]
  }' \
  "http://localhost:8080/exchanges/EXCHANGE_ID/counter-offer?userId=user-002"
```

**Contraoferta:** Solo "Matar un Ruiseñor" por solo "Cien Años de Soledad"

---

## 🧪 7. Ejemplos Adicionales de Intercambios

### Carlos (user-003) intercambia con Ana (user-004)
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "user-003",
    "ownerId": "user-004",
    "ownerBookIds": ["ub-018"],
    "requesterBookIds": ["ub-012"]
  }' \
  "http://localhost:8080/exchanges"
```
**Intercambio:** Carlos ofrece "Un Mundo Feliz" por "La Ladrona de Libros" de Ana

### Luis (user-005) intercambia con Carlos (user-003)
```bash
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "user-005", 
    "ownerId": "user-003",
    "ownerBookIds": ["ub-013"],
    "requesterBookIds": ["ub-022", "ub-025"]
  }' \
  "http://localhost:8080/exchanges"
```
**Intercambio:** Luis ofrece "Crimen y Castigo" + "El Gran Gatsby" por "Harry Potter" de Carlos

---

## 📊 8. Verificar libros disponibles para intercambio

### Ver todos los libros marcados para intercambio
```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/books/exchange"
```

### Ver biblioteca de un usuario específico
```bash
curl -X GET \
  -H "Accept: application/json" \
  "http://localhost:8080/books/users/user-001/library"
```

---

## 🚀 Script de Testing Automático

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== Testing BookExchange con datos reales ==="

# 1. Crear intercambio entre Juan y María
echo "1. Creando intercambio Juan -> María..."
EXCHANGE_1=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "user-001",
    "ownerId": "user-002",
    "ownerBookIds": ["ub-007", "ub-010"],
    "requesterBookIds": ["ub-002", "ub-005"]
  }' \
  "$BASE_URL/exchanges")

EXCHANGE_ID_1=$(echo $EXCHANGE_1 | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "Exchange 1 ID: $EXCHANGE_ID_1"
echo $EXCHANGE_1 | jq '.' 2>/dev/null || echo $EXCHANGE_1

# 2. Crear intercambio entre Carlos y Ana
echo -e "\n2. Creando intercambio Carlos -> Ana..."
EXCHANGE_2=$(curl -s -X POST \
  -H "Content-Type: application/json" \
  -d '{
    "requesterId": "user-003",
    "ownerId": "user-004", 
    "ownerBookIds": ["ub-018"],
    "requesterBookIds": ["ub-012"]
  }' \
  "$BASE_URL/exchanges")

EXCHANGE_ID_2=$(echo $EXCHANGE_2 | grep -o '"id":"[^"]*"' | cut -d'"' -f4)
echo "Exchange 2 ID: $EXCHANGE_ID_2"

# 3. María acepta el intercambio
echo -e "\n3. María acepta el intercambio..."
curl -s -X PUT \
  -H "Content-Type: application/json" \
  -d '{"status": "ACCEPTED"}' \
  "$BASE_URL/exchanges/$EXCHANGE_ID_1/status?userId=user-002" | jq '.' 2>/dev/null

# 4. Ana hace contraoferta
echo -e "\n4. Ana hace contraoferta..."
curl -s -X PUT \
  -H "Content-Type: application/json" \
  -d '{
    "ownerBookIds": ["ub-017"],
    "requesterBookIds": ["ub-012"]
  }' \
  "$BASE_URL/exchanges/$EXCHANGE_ID_2/counter-offer?userId=user-004" | jq '.' 2>/dev/null

# 5. Ver estado final
echo -e "\n5. Intercambios de Juan:"
curl -s -X GET "$BASE_URL/exchanges/users/user-001" | jq '.' 2>/dev/null

echo -e "\n6. Intercambios pendientes de Ana:"
curl -s -X GET "$BASE_URL/exchanges/users/user-004/pending-count"

echo -e "\n=== Testing Completado ==="
```

---

## 📝 Datos de Referencia Rápida

### Estados Válidos:
- `PENDING`, `ACCEPTED`, `REJECTED`, `COUNTERED`, `CANCELLED`, `COMPLETED`

### UserBooks para Testing Rápido:
```
ub-002, ub-005  # Juan (user-001): Gran Gatsby, Cien Años
ub-007, ub-010  # María (user-002): Ruiseñor, Dorian Gray  
ub-012, ub-013  # Carlos (user-003): Mundo Feliz, Harry Potter
ub-017, ub-018  # Ana (user-004): Harry Potter, Ladrona Libros
ub-022, ub-025  # Luis (user-005): Crimen Castigo, Gran Gatsby
```

### Verificación de Datos:
```sql
-- Ver user_books disponibles para intercambio
SELECT ub.id, u.username, b.title, ub.status 
FROM user_books ub 
JOIN users u ON ub.user_id = u.id 
JOIN books b ON ub.book_id = b.id 
WHERE ub.wants_to_exchange = true;
```