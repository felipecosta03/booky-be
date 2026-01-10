# Booky Backend (booky-be)

Backend de Booky: plataforma social de gestión de libros, clubes de lectura, comunidades, gamificación y generación de imágenes con IA.

## Descripción
Booky es una plataforma social para lectores que permite gestionar bibliotecas personales, participar en comunidades, clubes de lectura, intercambiar libros, comentar, chatear, gamificar la experiencia y generar imágenes de escenas literarias usando IA (OpenAI). Este repositorio contiene el backend desarrollado en Java con Spring Boot.

## Características principales
- Gestión de usuarios y autenticación JWT
- Biblioteca personal y gestión de libros (integración con Google Books)
- Clubes de lectura y reuniones virtuales (LiveKit)
- Comunidades y foros
- Intercambio de libros entre usuarios
- Sistema de comentarios y posts
- Chat en tiempo real
- Gamificación (logros, puntos, actividades)
- Generación de imágenes de escenas literarias con IA (OpenAI)
- Almacenamiento de imágenes en Cloudinary o AWS S3
- API documentada con OpenAPI/Swagger

## Instalación
### Requisitos previos
- Java 17+
- Maven 3.8+
- Docker (opcional, para despliegue y pruebas locales)
- Acceso a claves de Cloudinary, AWS S3, OpenAI y LiveKit (opcional para funcionalidades avanzadas)

### Clonar el repositorio
```bash
git clone https://github.com/felipecosta03/booky-be.git
cd booky-be
```

### Configuración
Las variables de entorno y configuraciones se definen en `src/main/resources/application.yml` y `application-prod.yml`. Puedes sobrescribirlas con variables de entorno.

Ejemplo de variables importantes:
- `DATABASE_URL` (producción)
- `JWT_SECRET`, `JWT_EXPIRATION`
- `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- `AWS_S3_ACCESS_KEY`, `AWS_S3_SECRET_KEY`, `AWS_S3_BUCKET`
- `OPENAI_API_KEY`
- `LIVEKIT_API_KEY`, `LIVEKIT_API_SECRET`, `LIVEKIT_WS_URL`

Puedes copiar y adaptar el archivo `application.yml` para desarrollo local.

## Ejecución local
### Usando Maven
```bash
./mvnw spring-boot:run
```
La API estará disponible en http://localhost:8080

### Usando Docker
```bash
docker build -t booky-be .
docker run -p 8080:8080 --env-file .env booky-be
```

## Testing
Ejecuta los tests con:
```bash
./mvnw test
```
Los tests cubren servicios, controladores y adaptadores de infraestructura.

## Despliegue
### Docker Compose
El proyecto soporta despliegue con Docker Compose (ver workflows en `.github/workflows/`).

### Fly.io / EC2
- Configura las variables de entorno necesarias.
- Usa los archivos de workflow para automatizar el despliegue.

## Documentación de la API
La documentación OpenAPI/Swagger está disponible en:
- http://localhost:8080/swagger-ui.html
- http://localhost:8080/v3/api-docs

Incluye endpoints para usuarios, libros, comunidades, clubes de lectura, posts, comentarios, chat, gamificación, imágenes y más.

## Estructura de carpetas
- `src/main/java/com/uade/bookybe/` — Código fuente principal
- `src/main/resources/` — Configuración y recursos
- `src/test/java/` — Tests unitarios e integración
- `Dockerfile` — Imagen Docker
- `pom.xml` — Dependencias Maven

## Ejemplo de uso de la API
```http
POST /auth/login
{
  "email": "usuario@ejemplo.com",
  "password": "123456"
}
// Respuesta: { "token": "..." }

GET /books/search?q=harry+potter
// Respuesta: lista de libros
```