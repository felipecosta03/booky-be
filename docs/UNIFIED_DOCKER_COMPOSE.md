# Docker Compose Unificado

## 🎯 Objetivo

Unificar `docker-compose.yml` y `docker-compose-ec2.yml` en un solo archivo que se adapte automáticamente a diferentes entornos usando variables de entorno.

## 📁 Estructura de Archivos

```
booky-be/
├── docker-compose.yml          # Archivo unificado
├── env.local                   # Configuración para desarrollo
├── env.production              # Configuración para producción
├── env.example                 # Ejemplo de configuración
└── docs/
    └── UNIFIED_DOCKER_COMPOSE.md
```

## 🚀 Uso

### Para Desarrollo Local

```bash
# Usar configuración por defecto (desarrollo)
docker-compose up -d

# O especificar archivo de configuración
docker-compose --env-file env.local up -d
```

### Para Producción EC2

```bash
# Usar archivo de configuración de producción
docker-compose --env-file env.production up -d

# O exportar variables de entorno
export $(cat env.production | xargs)
docker-compose up -d
```

### Para GitHub Actions

```yaml
- name: Deploy to EC2
  run: |
    docker-compose --env-file env.production up -d
```

## 🔧 Variables de Entorno

### Principales Variables

| Variable | Desarrollo | Producción | Descripción |
|----------|------------|------------|-------------|
| `DOCKER_IMAGE` | `booky-be_booky-app:latest` | `bookypfi/booky-backend:latest` | Imagen de Docker |
| `SPRING_PROFILES_ACTIVE` | `local` | `prod` | Perfil de Spring |
| `SECURITY_ENABLED` | `false` | `true` | Activar seguridad |
| `IMAGE_STORAGE_STRATEGY` | `cloudinary` | `cloudinary` o `s3` | Estrategia de almacenamiento |

### Configuración de Cloudinary

```bash
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

### Configuración de AWS S3

```bash
AWS_S3_ACCESS_KEY=your-access-key
AWS_S3_SECRET_KEY=your-secret-key
AWS_S3_REGION=us-east-1
AWS_S3_BUCKET=bucket-user-images-store
```

## 🎨 Ventajas del Sistema Unificado

### ✅ **Beneficios**

1. **Un solo archivo**: Mantenimiento más fácil
2. **Flexibilidad**: Adaptable a cualquier entorno
3. **Valores por defecto**: Configuración de desarrollo sin configuración
4. **Seguridad**: Variables sensibles en archivos separados
5. **Compatibilidad**: Funciona con scripts existentes

### ⚡ **Casos de Uso**

```bash
# Desarrollo local con Cloudinary
docker-compose up -d

# Desarrollo local con S3
IMAGE_STORAGE_STRATEGY=s3 docker-compose up -d

# Producción con configuración específica
docker-compose --env-file env.production up -d

# Testing con configuración custom
docker-compose --env-file env.test up -d
```

## 📂 Migración desde Archivos Separados

### Antes
```bash
# Desarrollo
docker-compose up -d

# Producción
docker-compose -f docker-compose-ec2.yml up -d
```

### Después
```bash
# Desarrollo (sin cambios)
docker-compose up -d

# Producción (con archivo de configuración)
docker-compose --env-file env.production up -d
```

## 🔒 Seguridad

### Archivos a Ignorar en Git

```gitignore
.env
.env.local
.env.production
env.local
env.production
```

### Variables Sensibles

- `JWT_SECRET`: Usar secretos diferentes para producción
- `CLOUDINARY_API_SECRET`: No hardcodear en archivos
- `AWS_S3_SECRET_KEY`: Usar IAM roles cuando sea posible

## 🛠️ Troubleshooting

### Error: Variable not found

```bash
# Verificar variables de entorno
docker-compose config

# Verificar con archivo específico
docker-compose --env-file env.local config
```

### Error: Wrong image

```bash
# Verificar qué imagen se está usando
docker-compose config | grep image

# Construir imagen local si es necesario
docker build -t booky-be_booky-app:latest .
```

### Error: Service won't start

```bash
# Verificar logs
docker-compose logs booky-app

# Verificar configuración
docker-compose --env-file env.local config
```

## 📝 Scripts Útiles

### Desarrollo
```bash
#!/bin/bash
# dev.sh
echo "Starting development environment..."
docker-compose --env-file env.local up -d
```

### Producción
```bash
#!/bin/bash
# prod.sh
echo "Starting production environment..."
docker-compose --env-file env.production up -d
```

### Limpieza
```bash
#!/bin/bash
# clean.sh
echo "Cleaning up..."
docker-compose down -v
docker system prune -f
```

## 🔄 Compatibilidad Backwards

El sistema es 100% compatible con:
- ✅ Scripts existentes de desarrollo
- ✅ GitHub Actions workflows
- ✅ Comandos de Docker Compose estándar
- ✅ Variables de entorno del sistema

## 📈 Próximos Pasos

1. **Eliminar archivos obsoletos**: `docker-compose-ec2.yml`
2. **Actualizar documentación**: README.md
3. **Actualizar scripts**: `booky.sh`
4. **Actualizar GitHub Actions**: `.github/workflows/` 