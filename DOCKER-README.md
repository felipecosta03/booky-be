# 🚀 Booky Backend - Docker Setup

## 📋 Prerequisitos

- **Docker** instalado y ejecutándose (Docker Desktop o Colima)
- **Git** para clonar el repositorio

## 🏃‍♂️ Inicio Rápido

### Scripts Simplificados (Recomendado)

El proyecto incluye **4 scripts principales** optimizados para **Colima** y **Docker Desktop**:

```bash
# 🚀 Levantar toda la aplicación (primera vez)
./start.sh

# 🔄 Restart solo backend (para cambios de código)
./restart-app.sh  

# 📊 Ver estado y diagnóstico
./status.sh

# 🛑 Parar toda la aplicación
./stop.sh
```

### Comandos Docker Manuales (Alternativo)
```bash
# Opción 1: Usando docker-compose
docker-compose up --build -d

# Opción 2: Ver logs
docker-compose logs -f

# Opción 3: Parar aplicación
docker-compose down
```

## ⚡ **Tipos de Comandos**

| Script | Velocidad | Qué Hace | Cuándo Usar |
|---------|-----------|----------|-------------|
| `./start.sh` | ⚡⚡ Completo | Levanta BD + Backend + Adminer | Primera vez o después de stop |
| `./restart-app.sh` | ⚡⚡⚡ Súper Rápido | Solo rebuilda backend | Cambios de código únicamente |
| `./status.sh` | ⚡⚡⚡⚡ Instantáneo | Muestra estado y logs | Diagnóstico y monitoreo |
| `./stop.sh` | ⚡⚡⚡ Rápido | Para todos los servicios | Al terminar de trabajar |

## 🔄 Flujo de Desarrollo

| Situación | Comando Recomendado | Descripción |
|-----------|-------------------|-------------|
| 🆕 **Primera vez** | `./start.sh` | Levanta todo desde cero |
| 📝 **Cambios de código** | `./restart-app.sh` | Solo rebuilda app (⚡ súper rápido) |
| 🔍 **Ver estado** | `./status.sh` | Diagnóstico completo |
| 🛑 **Terminar trabajo** | `./stop.sh` | Para todos los servicios |
| 🧹 **Problemas graves** | `./stop.sh` → `./start.sh` | Reset completo |

## 🔧 Configuración Automática

Los scripts cargan automáticamente las variables del archivo **`.env`**:

```bash
# Variables principales usadas por los scripts
DATABASE_NAME=booky
DATABASE_USERNAME=postgres  
DATABASE_PASSWORD=admin
JWT_SECRET=your-jwt-secret-key
CLOUDINARY_CLOUD_NAME=your_cloud_name
# ... y todas las demás del archivo .env
```

## 🌐 URLs de la Aplicación

| Servicio | URL | Descripción |
|----------|-----|-------------|
| 🚀 **API REST** | http://localhost:8080 | Backend principal |
| 📚 **Swagger UI** | http://localhost:8080/swagger-ui/index.html | Documentación interactiva |
| 🗄️ **Adminer** | http://localhost:8081 | Administrador de BD |
| 📊 **PostgreSQL** | localhost:5433 | Base de datos (postgres/admin) |

## 🧪 Comandos de Prueba

### Buscar libros:
```bash
curl "http://localhost:8080/books/search?q=hobbit"
```

### Obtener libro por ISBN:
```bash
curl "http://localhost:8080/books/isbn/9780547928227"
```

### Agregar libro a biblioteca:
```bash
curl -X POST "http://localhost:8080/books/users/user-001/library" \
  -H "Content-Type: application/json" \
  -d '{
    "isbn": "9780547928227",
    "status": "TO_READ"
  }'
```

### Obtener biblioteca del usuario:
```bash
curl "http://localhost:8080/books/users/user-001/library"
```

### Registro de usuario:
```bash
curl -X POST "http://localhost:8080/sign-up" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Juan",
    "lastname": "Pérez", 
    "email": "juan@example.com",
    "username": "juanperez",
    "password": "SecurePass123!"
  }'
```

## 🛠️ Comandos Docker Útiles

### Ver estado:
```bash
# Estado de contenedores (script)
./status.sh

# Estado manual
docker ps --filter "name=booky"
```

### Ver logs:
```bash
# Logs del backend
docker logs booky-backend -f

# Logs de la base de datos  
docker logs booky-postgres -f

# Logs usando docker-compose
docker-compose logs -f
```

### Gestión de contenedores:
```bash
# Reiniciar solo backend (script recomendado)
./restart-app.sh

# Reiniciar manual
docker restart booky-backend

# Ver uso de recursos
docker stats --filter "name=booky"
```

### Limpiar recursos:
```bash
# Parar servicios (script recomendado)
./stop.sh

# Limpiar completamente (¡CUIDADO! Borra datos)
docker system prune -a --volumes
```

## 🐛 Solución de Problemas

### Error: "port is already allocated"
```bash
# Ver qué proceso usa el puerto
lsof -i :8080
lsof -i :5433

# Solución: cambiar puertos en docker-compose.yml o matar proceso
```

### Error: "Docker daemon not running"
- **Docker Desktop:** Abrir Docker Desktop y esperar que inicie
- **Colima:** Ejecutar `colima start`

### Aplicación no responde:
```bash
# 1. Ver estado
./status.sh

# 2. Ver logs para diagnosticar
docker logs booky-backend --tail 50

# 3. Reiniciar backend
./restart-app.sh

# 4. Si persiste, reset completo
./stop.sh && ./start.sh
```

### Base de datos no conecta:
```bash
# 1. Verificar que PostgreSQL esté corriendo
docker ps | grep postgres

# 2. Ver logs de la base de datos
docker logs booky-postgres --tail 20

# 3. Conectarse directamente para probar
docker exec -it booky-postgres psql -U postgres -d booky

# 4. Reset completo si es necesario
./stop.sh && ./start.sh
```

### Scripts no funcionan (permisos):
```bash
# Dar permisos de ejecución
chmod +x *.sh

# Verificar que bash esté disponible
which bash
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
2. **Sistema:** PostgreSQL
3. **Servidor:** postgres (o booky-postgres)
4. **Usuario:** postgres
5. **Contraseña:** admin
6. **Base de datos:** booky

## 📚 Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| **Libros** |
| GET | `/books/search?q={query}` | Buscar libros |
| GET | `/books/isbn/{isbn}` | Obtener libro por ISBN |
| GET | `/books/exchange` | Libros disponibles para intercambio |
| **Biblioteca de Usuario** |
| POST | `/books/users/{userId}/library` | Agregar libro a biblioteca |
| GET | `/books/users/{userId}/library` | Obtener biblioteca del usuario |
| GET | `/books/users/{userId}/favorites` | Obtener libros favoritos |
| PUT | `/books/users/{userId}/books/{bookId}/status` | Actualizar estado de lectura |
| PUT | `/books/users/{userId}/books/{bookId}/favorite` | Toggle favorito |
| **Usuarios** |
| POST | `/sign-up` | Registro de usuario |
| POST | `/sign-in` | Inicio de sesión |
| GET | `/users/{id}` | Obtener usuario por ID |
| PUT | `/users` | Actualizar perfil de usuario |
| GET | `/users/{id}/followers` | Obtener seguidores |
| GET | `/users/{id}/following` | Obtener usuarios seguidos |
| POST | `/users/{followerId}/follow/{followedId}` | Seguir usuario |

## 💡 Consejos para Desarrollo

1. **Para cambios frecuentes de código:** Usa `./restart-app.sh` (es muy rápido)
2. **Para diagnóstico:** Usa `./status.sh` regularmente  
3. **Al terminar el día:** Usa `./stop.sh` para liberar recursos
4. **Si algo va mal:** `./stop.sh` → `./start.sh` soluciona la mayoría de problemas

## 🎯 Scripts vs Docker Compose

| Característica | Scripts | Docker Compose |
|---------------|---------|----------------|
| **Facilidad** | ✅ Muy fácil | ⚠️ Requiere conocimiento |
| **Velocidad** | ✅ Optimizado | ⚠️ Más lento |
| **Variables .env** | ✅ Automático | ❌ Manual |
| **Diagnóstico** | ✅ `./status.sh` | ❌ Comandos manuales |
| **Colima** | ✅ Optimizado | ⚠️ Puede fallar |

**Recomendación:** Usa los scripts para desarrollo diario. Usa docker-compose solo si necesitas configuraciones avanzadas.

¡Listo para usar! 🎉 