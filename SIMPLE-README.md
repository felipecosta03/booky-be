# 🚀 Booky Backend - Scripts Simplificados

Scripts simplificados para gestionar la aplicación con **Colima** usando variables del archivo `.env`.

## 📋 Comandos Básicos

### 🚀 Levantar toda la aplicación (primera vez o completa)
```bash
./start.sh
```
- Levanta base de datos + backend + adminer
- **Carga variables del archivo `.env`**
- Construye la imagen del backend
- Configura la red y volúmenes

### 🔄 Restart solo del backend (para cambios en código)
```bash
./restart-app.sh
```
- Rebuilda y reinicia solo el backend
- **Usa variables del archivo `.env`**
- Mantiene la base de datos corriendo
- **Ideal para desarrollo** cuando cambias código

### 🛑 Parar toda la aplicación
```bash
./stop.sh
```
- Para y elimina todos los contenedores
- Mantiene los datos de la DB (volumen)
- Mantiene las imágenes para restart rápido

### 📊 Ver estado de la aplicación
```bash
./status.sh
```
- Muestra estado de contenedores
- Hace health check de servicios
- Muestra logs recientes

## 🌐 URLs de la Aplicación

| Servicio | URL | Descripción |
|----------|-----|-------------|
| 🚀 **API REST** | http://localhost:8080 | Backend principal |
| 📚 **Swagger UI** | http://localhost:8080/swagger-ui/index.html | Documentación interactiva |
| 🗄️ **Adminer** | http://localhost:8081 | Administrador de BD |
| 📊 **PostgreSQL** | localhost:5433 | Base de datos (postgres/admin) |

## 🔧 Configuración con .env

Los scripts cargan automáticamente las variables del archivo `.env`. 

**Variables principales:**
```bash
# Base de datos
DATABASE_NAME=booky
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=admin

# JWT
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000

# Cloudinary (opcional)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Logging
LOG_LEVEL=INFO
APP_LOG_LEVEL=DEBUG
SHOW_SQL=false

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

## 🔄 Flujo de Desarrollo Típico

1. **Primera vez:** `./start.sh`
2. **Cambios en código:** `./restart-app.sh`
3. **Ver estado:** `./status.sh`
4. **Parar todo:** `./stop.sh`

## 🧪 Comandos de Prueba

```bash
# Buscar libros
curl "http://localhost:8080/books/search?q=hobbit"

# Obtener libro por ISBN
curl "http://localhost:8080/books/isbn/9780547928227"

# Obtener usuario
curl "http://localhost:8080/users/user-001"

# Agregar libro a biblioteca
curl -X POST "http://localhost:8080/books/users/user-001/library" \
  -H "Content-Type: application/json" \
  -d '{"isbn":"9780547928227","status":"TO_READ"}'
```

## 🛠️ Comandos Útiles de Docker

```bash
# Ver logs en vivo del backend
docker logs booky-backend -f

# Ver todos los contenedores
docker ps

# Limpieza completa (¡cuidado! borra datos)
docker system prune -a --volumes
```

## 💡 Notas

- Los scripts están optimizados para **Colima**
- **Variables cargadas automáticamente** desde `.env`
- Los datos de la base de datos se mantienen entre reinicios
- Para desarrollo, usa `./restart-app.sh` para cambios rápidos
- Si tienes problemas, prueba `./stop.sh` y luego `./start.sh` 