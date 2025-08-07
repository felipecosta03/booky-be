# 🧪 Scripts de Testing - Booky Backend

Este directorio contiene scripts para crear datos de prueba y testear los endpoints de la aplicación.

## 📋 Orden de Ejecución

### 1. Preparación de Base de Datos
```bash
# 1. Ejecutar el schema base
psql -U postgres -d booky -f database_schema_updated.sql

# 2. Cargar usuarios base
psql -U postgres -d booky -f alta_usuarios.sql

# 3. Cargar libros y userbooks 
psql -U postgres -d booky -f alta_libros_userbooks.sql
```

### 2. Testing de Endpoints

#### Opción A: Testing Automático
```bash
# Script que ejecuta un flujo completo de testing
./quick_test_exchanges.sh

# Con URL personalizada
./quick_test_exchanges.sh http://tu-servidor:8080
```

#### Opción B: Testing Manual
```bash
# Consultar la documentación detallada
cat test_exchanges_curls_updated.md

# Ejemplos de curls individuales disponibles en el archivo
```

---

## 📁 Archivos Disponibles

### 📊 Scripts SQL de Datos
| Archivo | Descripción | Prerequisitos |
|---------|-------------|---------------|
| `database_schema_updated.sql` | Schema completo de la base de datos | Ninguno |
| `alta_usuarios.sql` | 16 usuarios con direcciones | Schema creado |
| `alta_libros_userbooks.sql` | 15 libros + 40 userbooks + exchanges | Usuarios creados |

### 🧪 Scripts de Testing
| Archivo | Descripción | Uso |
|---------|-------------|-----|
| `quick_test_exchanges.sh` | Testing automático de exchanges | `./quick_test_exchanges.sh` |
| `test_exchanges_curls_updated.md` | Curls manuales detallados | Consulta/copia |
| `create_userbooks.sh` | Script HTTP para crear userbooks | `./create_userbooks.sh [URL] [USER_ID]` |

---

## 🗃️ Datos Creados

### 👥 Usuarios (16 total)
- **admin-001**, **admin-002**: Administradores
- **user-001** a **user-016**: Usuarios de prueba con bibliotecas variadas

### 📚 Libros (15 total)
- **book-001** a **book-015**: Clásicos y contemporáneos
- Incluye: ISBN, título, autor, sinopsis, categorías
- Ejemplos: Orgullo y Prejuicio, 1984, Harry Potter, etc.

### 📖 UserBooks (40 total)
- **ub-001** a **ub-040**: Distribuidos entre 8 usuarios
- Estados: `READ`, `READING`, `TO_READ`, `WISHLIST`
- Algunos marcados como favoritos e intercambiables

---

## 🔄 Testing de Intercambios

### Usuarios con Libros para Intercambio

| Usuario | UserBook IDs | Libros Disponibles |
|---------|--------------|-------------------|
| **user-001** (Juan) | `ub-002`, `ub-005` | El Gran Gatsby, Cien Años de Soledad |
| **user-002** (María) | `ub-007`, `ub-010` | Matar un Ruiseñor, Dorian Gray |
| **user-003** (Carlos) | `ub-012`, `ub-013` | Un Mundo Feliz, Harry Potter |
| **user-004** (Ana) | `ub-017`, `ub-018` | Harry Potter, La Ladrona de Libros |
| **user-005** (Luis) | `ub-022`, `ub-025` | Crimen y Castigo, El Gran Gatsby |

### Ejemplos de Intercambios
```bash
# Juan intercambia con María
curl -X POST -H "Content-Type: application/json" \
  -d '{
    "requesterId": "user-001",
    "ownerId": "user-002", 
    "ownerBookIds": ["ub-007", "ub-010"],
    "requesterBookIds": ["ub-002", "ub-005"]
  }' \
  "http://localhost:8080/exchanges"
```

---

## 🚀 Comandos Rápidos

### Verificar Datos
```bash
# Ver usuarios
psql -U postgres -d booky -c "SELECT id, username, name FROM users LIMIT 5;"

# Ver libros
psql -U postgres -d booky -c "SELECT id, title, author FROM books LIMIT 5;"

# Ver userbooks intercambiables
psql -U postgres -d booky -c "
SELECT ub.id, u.username, b.title 
FROM user_books ub 
JOIN users u ON ub.user_id = u.id 
JOIN books b ON ub.book_id = b.id 
WHERE ub.wants_to_exchange = true;"
```

### Testing Endpoints
```bash
# Ver libros para intercambio
curl "http://localhost:8080/books/exchange"

# Ver biblioteca de un usuario
curl "http://localhost:8080/books/users/user-001/library"

# Ver intercambios de un usuario
curl "http://localhost:8080/exchanges/users/user-001"
```

---

## 🔧 Troubleshooting

### Problemas Comunes

1. **Error de conexión a base de datos**
   ```bash
   # Verificar que PostgreSQL esté corriendo
   docker ps | grep postgres
   # o
   brew services list | grep postgresql
   ```

2. **Usuarios no existen**
   ```bash
   # Ejecutar primero el script de usuarios
   psql -U postgres -d booky -f alta_usuarios.sql
   ```

3. **UserBooks no existen**
   ```bash
   # Ejecutar el script de libros
   psql -U postgres -d booky -f alta_libros_userbooks.sql
   ```

4. **Aplicación no responde**
   ```bash
   # Verificar que la app esté corriendo en el puerto correcto
   curl http://localhost:8080/actuator/health
   ```

### Limpiar Datos
```sql
-- Limpiar intercambios
DELETE FROM exchange_requester_books;
DELETE FROM exchange_owner_books;
DELETE FROM book_exchanges;

-- Limpiar userbooks
DELETE FROM user_books;

-- Limpiar libros
DELETE FROM book_categories;
DELETE FROM books;

-- Limpiar usuarios (CUIDADO: afecta otras tablas)
DELETE FROM users WHERE id LIKE 'user-%';
```

---

## 📝 Notas Adicionales

- **Contraseñas**: Todos los usuarios tienen la contraseña `password123`
- **Monedas**: Los usuarios tienen entre 90-300 monedas iniciales
- **Direcciones**: Distribuidas en Argentina, España, México, etc.
- **Estados de Intercambio**: `PENDING`, `ACCEPTED`, `REJECTED`, `COUNTERED`, `CANCELLED`, `COMPLETED`

Para más detalles sobre la API, consulta `test_exchanges_curls_updated.md`.