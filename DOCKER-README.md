# 🚀 Booky Backend - Docker Setup

## 📋 Prerequisitos

- **Docker Desktop** instalado y ejecutándose
- **Docker Compose** (incluido con Docker Desktop)

## 🏃‍♂️ Inicio Rápido

### Opción 1: Script Automático (Recomendado)

**Linux/macOS:**
```bash
./docker-run.sh
```

**Windows:**
```batch
docker-run.bat
```

### Opción 2: Inicio Ultra-Rápido
```bash
./quick-start.sh
```

### Opción 3: Rebuild Completo
```bash
./quick-rebuild.sh
```

### Opción 4: Rebuild Solo App (Rápido)
```bash
./quick-app-rebuild.sh
```

### Opción 5: Comandos Manuales
```bash
# Construir y ejecutar
docker-compose up --build -d

# Rebuild completo (elimina todo y reconstruye)
docker-compose down -v --rmi all --remove-orphans
docker system prune -f
docker-compose up --build -d

# Ver logs
docker-compose logs -f

# Parar aplicación
docker-compose down
```

## ⚡ **Tipos de Rebuild**

| Comando | Velocidad | Qué Rebuilda | Cuándo Usar |
|---------|-----------|--------------|-------------|
| `./quick-app-rebuild.sh` | ⚡⚡⚡ Súper Rápido | Solo la aplicación Java | Cambios de código únicamente |
| `./docker-run.sh restart` | ⚡⚡ Rápido | Aplicación + dependencias | Cambios en dependencies/config |
| `./quick-rebuild.sh` | ⚡ Lento | Todo + BD desde cero | Reset completo / problemas serios |

## 🔄 Cuándo usar cada opción

| Situación | Comando Recomendado | Descripción |
|-----------|-------------------|-------------|
| 🆕 **Primera vez** | `./docker-run.sh` o `./quick-start.sh` | Inicio normal |
| 📝 **Cambios de código** | `./quick-app-rebuild.sh` | Solo rebuilda app (⚡ rápido) |
| 🔄 **Cambios mayores** | `./docker-run.sh restart` | Rebuild completo |
| 🐛 **Problemas de dependencies** | `./quick-rebuild.sh` | Limpieza total |
| 🗄️ **Reset de base de datos** | `./quick-rebuild.sh` | Elimina datos y recrea |
| 🔧 **Problemas de Docker** | `./docker-run.sh rebuild` | Limpieza completa |

## 🌐 URLs de la Aplicación

| Servicio | URL | Descripción |
|----------|-----|-------------|
| 🚀 **API REST** | http://localhost:8080 | Backend principal |
| 📚 **Swagger UI** | http://localhost:8080/swagger-ui/index.html | Documentación interactiva |
| 🗄️ **Adminer** | http://localhost:8081 | Administrador de BD |
| 📊 **PostgreSQL** | localhost:5433 | Base de datos (postgres/admin) |

## 🧪 Comandos de Prueba

### Agregar libro a biblioteca:
```bash
curl -X POST "http://localhost:8080/api/books/users/user-001/library" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "9780547928227",
    "status": "TO_READ"
  }'
```

### Obtener biblioteca del usuario:
```bash
curl "http://localhost:8080/api/books/users/user-001/library"
```

### Buscar libros:
```bash
curl "http://localhost:8080/api/books/search?q=hobbit"
```

### Obtener libro por ISBN:
```bash
curl "http://localhost:8080/api/books/isbn/9780547928227"
```

## 🛠️ Comandos Docker Útiles

### Ver estado de contenedores:
```bash
docker-compose ps
```

### Ver logs en tiempo real:
```bash
# Logs de la aplicación
docker-compose logs -f booky-app

# Logs de la base de datos
docker-compose logs -f postgres

# Todos los logs
docker-compose logs -f
```

### Reiniciar servicios:
```bash
# Reiniciar todo
docker-compose restart

# Reiniciar solo la aplicación
docker-compose restart booky-app

# Rebuild completo (usando script)
./docker-run.sh rebuild

# Rebuild rápido (todo)
./quick-rebuild.sh

# Rebuild solo app (cambios de código)
./docker-run.sh rebuild-app
./quick-app-rebuild.sh
```

### Limpiar recursos:
```bash
# Parar y eliminar contenedores
docker-compose down

# Parar, eliminar contenedores y volúmenes
docker-compose down -v

# Limpiar recursos no utilizados
docker system prune -f
```

## 🐛 Solución de Problemas

### Error: "port is already allocated"
```bash
# Ver qué proceso usa el puerto
lsof -i :8080

# Cambiar puerto en docker-compose.yml
ports:
  - "8081:8080"  # Cambiar primer número
```

### Error: "Docker daemon not running"
- Abrir Docker Desktop
- Esperar a que inicie completamente
- Intentar de nuevo

### Aplicación no responde:
```bash
# Ver logs para diagnosticar
docker-compose logs booky-app

# Verificar estado de contenedores
docker-compose ps

# Reiniciar si es necesario
docker-compose restart booky-app
```

### Base de datos no conecta:
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose ps postgres

# Ver logs de la base de datos
docker-compose logs postgres

# Conectarse directamente para probar
docker-compose exec postgres psql -U postgres -d booky
```

## 📊 Configuración de Base de Datos

### Conexión desde herramientas externas:
- **Host:** localhost
- **Puerto:** 5433
- **Usuario:** postgres
- **Contraseña:** admin
- **Base de datos:** booky

### Adminer (Interfaz web):
1. Ir a http://localhost:8081
2. Sistema: PostgreSQL
3. Servidor: postgres
4. Usuario: postgres
5. Contraseña: admin
6. Base de datos: booky

## 🔧 Variables de Entorno

El archivo `docker-compose.yml` incluye configuración por defecto. Para personalizar:

1. Crear archivo `.env`:
```env
# JWT Configuration
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters
JWT_EXPIRATION=86400000

# Cloudinary (opcional)
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret

# Logging
LOG_LEVEL=DEBUG
SHOW_SQL=true

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

2. Reiniciar aplicación:
```bash
docker-compose down
docker-compose up -d
```

## 📚 Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/books/users/{userId}/library` | Agregar libro a biblioteca |
| GET | `/api/books/users/{userId}/library` | Obtener biblioteca del usuario |
| GET | `/api/books/users/{userId}/favorites` | Obtener libros favoritos |
| GET | `/api/books/search?q={query}` | Buscar libros |
| GET | `/api/books/isbn/{isbn}` | Obtener libro por ISBN |
| PUT | `/api/books/users/{userId}/books/{bookId}/status` | Actualizar estado |
| PUT | `/api/books/users/{userId}/books/{bookId}/favorite` | Toggle favorito |
| GET | `/api/books/exchange` | Libros para intercambio |

¡Listo para usar! 🎉 