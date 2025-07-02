# 📚 Booky Backend

Sistema backend para aplicación de intercambio y gestión de libros desarrollado con Spring Boot.

## 🚀 Inicio Rápido

### Prerrequisitos
- Docker y Docker Compose instalados
- Java 17+ (para desarrollo)
- Maven 3.8+ (para desarrollo)

### Opciones de Inicio

#### Opción 1: Script de Control Unificado (Recomendado)

Usa el script `booky.sh` para manejar toda la aplicación:

```bash
# Hacer el script ejecutable (solo primera vez)
chmod +x booky.sh

# Ver todos los comandos disponibles
./booky.sh
```

#### Opción 2: Docker Compose (Alternativa)

```bash
# Iniciar todos los servicios
docker-compose up -d

# Ver logs
docker-compose logs -f

# Parar servicios
docker-compose down
```

## 📋 Comandos Disponibles

### 1️⃣ Inicio Completo
```bash
./booky.sh start
```
- **Función**: Setup completo desde cero
- **Incluye**: PostgreSQL + Backend + Datos de muestra + Adminer
- **Tiempo**: ~2-3 minutos
- **Uso**: Primera ejecución o reset completo

### 2️⃣ Solo Backend (Desarrollo Rápido)
```bash
./booky.sh backend
```
- **Función**: Reconstruye solo el backend
- **Prerrequisito**: PostgreSQL debe estar corriendo
- **Tiempo**: ~1 minuto
- **Uso**: Ideal después de cambios en código

### 3️⃣ Parar Todo
```bash
./booky.sh stop
```
- **Función**: Para todos los servicios y limpia contenedores
- **Tiempo**: ~10 segundos
- **Uso**: Cierre limpio al terminar

## 🌐 Servicios Disponibles

Una vez iniciado, tendrás acceso a:

| Servicio | URL | Descripción |
|----------|-----|-------------|
| **API Backend** | http://localhost:8080 | API REST principal |
| **Swagger UI** | http://localhost:8080/swagger-ui/index.html | Documentación interactiva |
| **Adminer** | http://localhost:8081 | Cliente web PostgreSQL |
| **PostgreSQL** | localhost:5433 | Base de datos (postgres/admin) |

## 🧪 Endpoints de Prueba

```bash
# Buscar libros
curl "http://localhost:8080/books/search?q=hobbit"

# Obtener usuarios
curl "http://localhost:8080/users"

# Obtener comunidades
curl "http://localhost:8080/reading-clubs"
```

## 🏗️ Arquitectura

### Estructura del Proyecto
```
src/main/java/com/uade/bookybe/
├── config/           # Configuraciones (Security, JWT, etc.)
├── core/            # Modelos de dominio y casos de uso
├── infraestructure/ # Entidades, repositorios y adaptadores
└── router/          # Controladores y DTOs
```

### Capas de la Aplicación

1. **Controller**: Recibe DTOs, los mapea a modelos via MapStruct
2. **Service**: Lógica de negocio, comunica con repositorios
3. **Repository**: Acceso a datos, maneja entidades
4. **Mappers**: MapStruct para conversión DTO ↔ Modelo ↔ Entidad

## 🗄️ Base de Datos

### Esquema Principal
- **users**: Usuarios del sistema
- **books**: Catálogo de libros
- **user_books**: Biblioteca personal de cada usuario
- **community**: Comunidades de lectores
- **reading_clubs**: Clubes de lectura
- **reading_club_members**: Membresías de clubes

### Datos de Muestra Incluidos
- **16 usuarios** (incluye administradores)
- **20 comunidades** organizadas por géneros literarios
- **5 clubes de lectura** activos
- **5 libros** con categorías

## 🔧 Desarrollo

### Flujo Típico de Desarrollo
```bash
# Primera vez
./booky.sh start

# Hacer cambios en código...
./booky.sh backend    # Rebuild rápido

# Más cambios...
./booky.sh backend    # Rebuild rápido

# Al terminar
./booky.sh stop
```

### Variables de Entorno
```env
DATABASE_URL=jdbc:postgresql://booky-postgres:5432/booky
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=admin
SPRING_PROFILES_ACTIVE=local
```

### Autenticación JWT
La aplicación utiliza JWT para autenticación:
- **Sign-up**: `POST /sign-up`
- **Sign-in**: `POST /sign-in` (retorna JWT token)
- **Headers**: `Authorization: Bearer <token>`

## 📊 Funcionalidades Principales

### ✅ Implementadas y Verificadas
- **Gestión de Usuarios**: Registro, login, perfiles
- **Catálogo de Libros**: Búsqueda via Google Books API
- **Biblioteca Personal**: Agregar/remover libros, estados de lectura
- **Comunidades**: Creación y gestión de comunidades temáticas
- **Clubes de Lectura**: Clubes por libro con moderadores
- **Autenticación**: JWT con Spring Security

### 🔒 Endpoints Protegidos
- Todas las operaciones de biblioteca personal
- Gestión de comunidades
- Operaciones de clubes de lectura

## 🐛 Solución de Problemas

### El backend no inicia
```bash
# Ver logs
docker logs booky-backend

# Verificar PostgreSQL
docker ps | grep booky-postgres
```

### Base de datos no conecta
```bash
# Reiniciar todo desde cero
./booky.sh stop
./booky.sh start
```

### Puerto ya en uso
```bash
# Verificar qué está usando el puerto
lsof -i :8080
lsof -i :5433
```

## 📂 Scripts de Base de Datos

Los siguientes scripts se cargan automáticamente:

- `scripts/database_schema_updated.sql` - Esquema principal
- `scripts/alta_usuarios.sql` - Datos de usuarios
- `scripts/alta_comunidades.sql` - Datos de comunidades  
- `scripts/alta_clubes_lectura.sql` - Datos de clubes

## 🎯 Tecnologías

- **Backend**: Spring Boot 3.5.0, Java 17
- **Base de Datos**: PostgreSQL 15
- **Seguridad**: Spring Security + JWT
- **Documentación**: SpringDoc OpenAPI (Swagger)
- **Mapeo**: MapStruct
- **Contenedores**: Docker + Docker Compose
- **Build**: Maven

## 📝 Notas

- **Contraseña por defecto**: Todos los usuarios de muestra tienen la contraseña `password123`
- **Admin users**: `admin@booky.com` y `superadmin@booky.com`
- **Network**: Los contenedores usan la red `booky-network`
- **Persistencia**: Los datos de PostgreSQL se mantienen en el volumen `postgres_data`

---

**¿Problemas?** Usa `./booky.sh` para ver la ayuda completa o verifica los logs con `docker logs booky-backend` 