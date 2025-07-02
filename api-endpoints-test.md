# 🚀 Booky-BE API Endpoints Testing Guide

Este archivo contiene todos los endpoints de la API con ejemplos reales usando los datos de muestra que se cargan al iniciar la aplicación.

## 📋 Datos de Muestra Disponibles

### Usuarios:
- `user-001` (juanp) - juan.perez@gmail.com
- `user-002` (mariag) - maria.garcia@outlook.com  
- `user-003` (carlosr) - carlos.rodriguez@yahoo.com
- `admin-001` (admin) - admin@booky.com

### Comunidades:
- `comm-001` - Literatura Clásica
- `comm-002` - Ciencia Ficción
- `comm-003` - Historia y Biografías

### Reading Clubs:
- `rc-001`, `rc-002`, `rc-003`, `rc-004`, `rc-005`

### Libros Existentes:
- `book-001` - To Kill a Mockingbird (ISBN: 9780060935467)
- `book-002` - Harry Potter and the Philosopher's Stone (ISBN: 9780747532699)
- `book-c4da9477` - The Hobbit (ISBN: 054792822X)

---

## 📚 BOOKS ENDPOINTS

### 🔍 Search Books
```bash
# Buscar libros por término
curl "http://localhost:8080/books/search?q=hobbit"
curl "http://localhost:8080/books/search?q=harry"
curl "http://localhost:8080/books/search?q=fiction"
```

### 📖 Get Book by ISBN
```bash
# Obtener libro por ISBN
curl "http://localhost:8080/books/isbn/9780060935467"  # To Kill a Mockingbird
curl "http://localhost:8080/books/isbn/9780747532699"  # Harry Potter
curl "http://localhost:8080/books/isbn/054792822X"     # The Hobbit
```

### 🔄 Books Available for Exchange
```bash
# Obtener libros disponibles para intercambio
curl "http://localhost:8080/books/exchange"
```

---

## 👤 USERS ENDPOINTS

### 👥 Get All Users
```bash
# Obtener todos los usuarios
curl "http://localhost:8080/users"
```

### 🔍 Get Specific User
```bash
# Obtener usuario específico
curl "http://localhost:8080/users/user-001"  # Juan Pérez
curl "http://localhost:8080/users/user-002"  # María García
curl "http://localhost:8080/users/admin-001" # Admin
```

---

## 🔐 AUTHENTICATION ENDPOINTS

### 📝 Sign Up (Create New User)
```bash
# Crear nuevo usuario
curl -X POST "http://localhost:8080/sign-up" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser123",
    "password": "password123",
    "name": "Nuevo",
    "lastname": "Usuario",
    "email": "nuevo@example.com",
    "description": "Usuario de prueba",
    "address": {
      "state": "Buenos Aires",
      "country": "Argentina",
      "longitude": -58.3816,
      "latitude": -34.6037
    }
  }'
```

### 🔑 Sign In (Login)
```bash
# Iniciar sesión (NOTA: Actualmente retorna 401 - issue conocido)
curl -X POST "http://localhost:8080/sign-in" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan.perez@gmail.com",
    "password": "password123"
  }'

# Probar con admin
curl -X POST "http://localhost:8080/sign-in" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@booky.com",
    "password": "admin123"
  }'
```

---

## 📚 LIBRARY OPERATIONS

### ➕ Add Book to User Library
```bash
# Agregar libro a biblioteca del usuario
curl -X POST "http://localhost:8080/books/users/user-001/library" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "9780439708180",
    "status": "TO_READ"
  }'

# Agregar The Hobbit (ya existe - debería reutilizar)
curl -X POST "http://localhost:8080/books/users/user-001/library" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "9780547928227",
    "status": "WANT_TO_READ"
  }'
```

### 📚 Get User Library
```bash
# Obtener biblioteca del usuario
curl "http://localhost:8080/books/users/user-001/library"
curl "http://localhost:8080/books/users/user-002/library"
```

### ⭐ Get User Favorites
```bash
# Obtener libros favoritos del usuario
curl "http://localhost:8080/books/users/user-001/favorites"
curl "http://localhost:8080/books/users/user-002/favorites"
```

### 📊 Update Book Status
```bash
# Cambiar estado de libro (TO_READ, READING, READ, WANT_TO_READ)
curl -X PUT "http://localhost:8080/books/users/user-001/books/book-c4da9477/status" \
  -H "Content-Type: application/json" \
  -d '{"status": "READING"}'

curl -X PUT "http://localhost:8080/books/users/user-001/books/book-c4da9477/status" \
  -H "Content-Type: application/json" \
  -d '{"status": "READ"}'
```

### ⭐ Toggle Book Favorite
```bash
# Marcar/desmarcar libro como favorito
curl -X PUT "http://localhost:8080/books/users/user-001/books/book-c4da9477/favorite"
```

### 🔄 Update Exchange Preference
```bash
# Marcar libro para intercambio
curl -X PUT "http://localhost:8080/books/users/user-001/books/book-c4da9477/exchange" \
  -H "Content-Type: application/json" \
  -d '{"wants_to_exchange": true}'

# Desmarcar libro para intercambio
curl -X PUT "http://localhost:8080/books/users/user-001/books/book-c4da9477/exchange" \
  -H "Content-Type: application/json" \
  -d '{"wants_to_exchange": false}'
```

---

## 🏘️ COMMUNITIES ENDPOINTS

### 🌍 Get All Communities
```bash
# Obtener todas las comunidades
curl "http://localhost:8080/communities"
```

### 🔍 Get Specific Community
```bash
# Obtener comunidad específica
curl "http://localhost:8080/communities/comm-001"  # Literatura Clásica
curl "http://localhost:8080/communities/comm-002"  # Ciencia Ficción
curl "http://localhost:8080/communities/comm-003"  # Historia y Biografías
```

---

## 📖 READING CLUBS ENDPOINTS

### 📚 Get All Reading Clubs
```bash
# Obtener todos los clubs de lectura
curl "http://localhost:8080/reading-clubs"
```

### 🔍 Get Specific Reading Club
```bash
# Obtener club específico
curl "http://localhost:8080/reading-clubs/rc-001"
curl "http://localhost:8080/reading-clubs/rc-002"
```

### 👥 Get Reading Clubs by User
```bash
# Obtener clubs del usuario
curl "http://localhost:8080/reading-clubs/user/user-001"
curl "http://localhost:8080/reading-clubs/user/user-002"
```

### 🌍 Get Reading Clubs by Community
```bash
# Obtener clubs de una comunidad
curl "http://localhost:8080/reading-clubs/community/comm-001"
curl "http://localhost:8080/reading-clubs/community/comm-002"
```

---

## 📝 POSTS ENDPOINTS

### 📋 Get All Posts
```bash
# Obtener todos los posts
curl "http://localhost:8080/posts"
```

### 🌍 Get Posts by Community
```bash
# Obtener posts de una comunidad
curl "http://localhost:8080/posts/community/comm-001"
curl "http://localhost:8080/posts/community/comm-002"
```

### 👤 Get Posts by User
```bash
# Obtener posts de un usuario
curl "http://localhost:8080/posts/user/user-001"
curl "http://localhost:8080/posts/user/user-002"
```

---

## 🧪 TESTING SCENARIOS

### Scenario 1: Usuario Nuevo Registra y Agrega Libros
```bash
# 1. Crear usuario
curl -X POST "http://localhost:8080/sign-up" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "test123",
    "name": "Test",
    "lastname": "User",
    "email": "test@test.com",
    "description": "Test user",
    "address": {
      "state": "Test State",
      "country": "Test Country",
      "longitude": -58.3816,
      "latitude": -34.6037
    }
  }'

# 2. Obtener el ID del usuario creado (se mostrará en la respuesta)
# Usar ese ID en lugar de {NEW_USER_ID}

# 3. Agregar libro a su biblioteca
curl -X POST "http://localhost:8080/books/users/{NEW_USER_ID}/library" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "9780439708180",
    "status": "TO_READ"
  }'
```

### Scenario 2: Operaciones Completas en Biblioteca
```bash
# 1. Agregar libro
curl -X POST "http://localhost:8080/books/users/user-001/library" \
  -H "Content-Type: application/json" \
  -d '{"isbn": "9780316769488", "status": "TO_READ"}'

# 2. Ver biblioteca
curl "http://localhost:8080/books/users/user-001/library"

# 3. Cambiar estado a READING
curl -X PUT "http://localhost:8080/books/users/user-001/books/{BOOK_ID}/status" \
  -H "Content-Type: application/json" \
  -d '{"status": "READING"}'

# 4. Marcar como favorito
curl -X PUT "http://localhost:8080/books/users/user-001/books/{BOOK_ID}/favorite"

# 5. Marcar para intercambio
curl -X PUT "http://localhost:8080/books/users/user-001/books/{BOOK_ID}/exchange" \
  -H "Content-Type: application/json" \
  -d '{"wants_to_exchange": true}'

# 6. Verificar libros para intercambio
curl "http://localhost:8080/books/exchange"
```

---

## 🔧 UTILIDADES PARA TESTING

### Verificar Estado de la Aplicación
```bash
# Health check básico
curl "http://localhost:8080/users" | jq 'length'

# Verificar base de datos
curl "http://localhost:8080/communities" | jq 'length'
```

### Limpiar/Resetear Datos
```bash
# Para resetear datos, usar el script:
./booky.sh stop
./booky.sh start
```

### Ver Logs de la Aplicación
```bash
docker logs booky-backend --tail 50
```

---

## ⚠️ NOTAS IMPORTANTES

1. **Sign-in Issue**: El endpoint de login actualmente retorna 401. Investigar configuración de Spring Security.

2. **IDs Dinámicos**: Algunos IDs se generan automáticamente. Para obtener IDs reales:
   - Usuarios: `curl "http://localhost:8080/users" | jq '.[].id'`
   - Libros: Revisar respuesta de agregar a biblioteca
   - Comunidades: `curl "http://localhost:8080/communities" | jq '.[].id'`

3. **ISBNs Existentes**: Algunos libros ya existen en la BD. El sistema reutiliza libros existentes correctamente.

4. **Estados de Libros**: Los valores válidos son: `TO_READ`, `READING`, `READ`, `WANT_TO_READ`

---

## 📊 RESUMEN DE ENDPOINTS

- ✅ **16/17 endpoints funcionando** (94%)
- ❌ **1/17 con issues** (POST /sign-in)
- 📚 **Datos de muestra**: 21 usuarios, 20 comunidades, 5 reading clubs, 8 libros 