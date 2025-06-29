# 📚 Booky Backend

**Booky-BE** es una API REST desarrollada con Spring Boot para una plataforma de gestión de libros y lectura social.

## 🚀 Características Principales

- **👤 Gestión de Usuarios**: Registro, autenticación y perfiles de usuario
- **📖 Sistema de Seguimientos**: Los usuarios pueden seguir y ser seguidos
- **🖼️ Gestión de Imágenes**: Integración con Cloudinary para subida de imágenes
- **🔐 Seguridad**: Autenticación con Spring Security
- **📊 Base de Datos**: PostgreSQL con JPA/Hibernate
- **📚 Documentación**: API documentada con OpenAPI/Swagger

## 🛠️ Tecnologías Utilizadas

- **Java 17**
- **Spring Boot 3.5.0**
- **Spring Data JPA**
- **Spring Security**
- **PostgreSQL**
- **MapStruct** - Mapeo de objetos
- **Lombok** - Reducción de código boilerplate
- **OpenAPI/Swagger** - Documentación de API
- **Cloudinary** - Gestión de imágenes
- **Maven** - Gestión de dependencias

## 🏗️ Arquitectura

El proyecto sigue una arquitectura en capas con elementos de arquitectura hexagonal:

```
src/main/java/com/uade/bookybe/
├── config/              # Configuraciones (Security, OpenAPI, Exception Handler)
├── core/                # Dominio de la aplicación
│   ├── model/          # Modelos de dominio
│   ├── usecase/        # Casos de uso (Services)
│   ├── port/           # Puertos (Interfaces)
│   └── exception/      # Excepciones del dominio
├── infraestructure/     # Infraestructura
│   ├── entity/         # Entidades JPA
│   ├── repository/     # Repositorios
│   ├── adapter/        # Adaptadores
│   └── mapper/         # Mappers Entidad-Modelo
├── router/              # Capa de presentación
│   ├── dto/            # DTOs de entrada/salida
│   └── mapper/         # Mappers DTO-Modelo
└── util/               # Utilidades

```

## 🔧 Configuración y Ejecución

### Prerrequisitos

- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Cuenta de Cloudinary (opcional)

### Variables de Entorno

Configura las siguientes variables de entorno:

```bash
# Base de datos
DATABASE_URL=jdbc:postgresql://localhost:5432/booky
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
DATABASE_NAME=booky

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# JWT (opcional)
JWT_SECRET=your-jwt-secret-key
JWT_EXPIRATION=86400000

# Logging
LOG_LEVEL=INFO
APP_LOG_LEVEL=DEBUG

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:4200
```

### Ejecución Local

1. **Clonar el repositorio**
   ```bash
   git clone <repository-url>
   cd booky-be
   ```

2. **Configurar base de datos**
   ```sql
   CREATE DATABASE booky;
   ```

3. **Ejecutar la aplicación**
   ```bash
   mvn spring-boot:run
   ```

4. **Verificar funcionamiento**
   - API: http://localhost:8080/api/v1
   - Documentación: http://localhost:8080/swagger-ui.html

## 📖 API Documentation

### Endpoints Principales

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/api/v1/sign-up` | Registro de usuario |
| POST | `/api/v1/sign-in` | Inicio de sesión |
| GET | `/api/v1/users/{id}` | Obtener usuario por ID |
| PUT | `/api/v1/users` | Actualizar perfil de usuario |
| DELETE | `/api/v1/users/{id}` | Eliminar usuario |
| GET | `/api/v1/users/{id}/followers` | Obtener seguidores |
| GET | `/api/v1/users/{id}/following` | Obtener seguidos |
| POST | `/api/v1/users/{followerId}/follow/{followedId}` | Seguir usuario |
| DELETE | `/api/v1/users/{followerId}/follow/{followedId}` | Dejar de seguir |

### Ejemplo de Uso

**Registro de Usuario:**
```json
POST /api/v1/sign-up
{
  "name": "Juan",
  "lastname": "Pérez",
  "email": "juan.perez@example.com",
  "username": "juanperez",
  "password": "SecurePassword123!"
}
```

**Respuesta:**
```json
{
  "id": "123e4567-e89b-12d3-a456-426614174000",
  "name": "Juan",
  "lastname": "Pérez",
  "email": "juan.perez@example.com",
  "username": "juanperez",
  "date_created": "2025-01-15T10:30:00"
}
```

## 🔧 Mejoras Implementadas

### ✅ Configuración Mejorada
- Variables de entorno para configuración segura
- Logging estructurado y configurable
- Configuración de CORS y multipart

### ✅ Manejo de Excepciones
- Manejador global de excepciones (`@RestControllerAdvice`)
- Respuestas de error estandarizadas
- Logging detallado de errores

### ✅ Validaciones
- Validaciones con Bean Validation (JSR-303)
- DTOs con validaciones robustas
- Mensajes de error descriptivos

### ✅ Documentación API
- Integración completa con OpenAPI 3
- Documentación detallada de endpoints
- Ejemplos de request/response
- Esquemas de seguridad documentados

### ✅ Logging Mejorado
- Logs estructurados en todos los endpoints
- Diferentes niveles de log configurables
- Tracking de operaciones importantes

### ✅ Arquitectura Limpia
- Separación clara de responsabilidades
- Uso correcto de MapStruct para mapeos
- Inyección de dependencias optimizada

## 🧪 Testing

### Ejecutar Tests
```bash
mvn test
```

### Cobertura de Tests
```bash
mvn jacoco:report
```

## 📝 Buenas Prácticas Implementadas

1. **Clean Architecture**: Separación clara entre capas
2. **SOLID Principles**: Código mantenible y extensible
3. **DTOs**: Separación entre modelos de dominio y API
4. **Exception Handling**: Manejo centralizado de errores
5. **Logging**: Trazabilidad completa de operaciones
6. **Validation**: Validación robusta de entrada de datos
7. **Documentation**: API bien documentada
8. **Security**: Configuración segura con variables de entorno

## 🔐 Seguridad

- **Variables de Entorno**: Credenciales no hardcodeadas
- **Input Validation**: Validación robusta de todos los inputs
- **Error Handling**: No exposición de información sensible
- **CORS**: Configuración adecuada para cross-origin requests

## 🚀 Despliegue

### Docker (Próximamente)
```dockerfile
# Dockerfile de ejemplo
FROM openjdk:17-jdk-slim
COPY target/booky-be-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Variables de Producción
Asegúrate de configurar todas las variables de entorno en tu servidor de producción.

## 🤝 Contribución

1. Fork el proyecto
2. Crear rama feature (`git checkout -b feature/nueva-caracteristica`)
3. Commit cambios (`git commit -am 'Añade nueva característica'`)
4. Push a la rama (`git push origin feature/nueva-caracteristica`)
5. Crear Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT - ver el archivo [LICENSE](LICENSE) para detalles.

## 👥 Equipo

- **Desarrollador**: Tu Nombre
- **Email**: tu.email@example.com

---

⭐ **¡Dale una estrella al proyecto si te fue útil!** 