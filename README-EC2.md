# 🚀 Despliegue de Booky Backend en EC2

## ✅ Problema resuelto con plataforma AMD64

La imagen Docker ha sido construida específicamente para **AMD64** y funciona perfectamente en EC2.

## 📋 Requisitos previos

1. **EC2 con Amazon Linux 2 o Ubuntu**
2. **Security Groups configurados**:
   - Puerto 22 (SSH)
   - Puerto 8080 (Aplicación)
   - Puerto 5432 (PostgreSQL - opcional)
3. **Archivo .pem para SSH**

## 🔧 Opción 1: Despliegue Automático

### 1. Conectarse al EC2
```bash
# Dar permisos al archivo .pem
chmod 400 tu-key-pair.pem

# Conectarse al EC2
ssh -i tu-key-pair.pem ec2-user@tu-ip-publica
```

### 2. Descargar y ejecutar el script
```bash
# Descargar el script
curl -O https://raw.githubusercontent.com/tu-usuario/booky-be/main/deploy-to-ec2.sh

# Dar permisos de ejecución
chmod +x deploy-to-ec2.sh

# Ejecutar el script
./deploy-to-ec2.sh
```

## 🛠️ Opción 2: Despliegue Manual

### 1. Instalar Docker

#### Para Amazon Linux 2:
```bash
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ec2-user
```

#### Para Ubuntu:
```bash
sudo apt update
sudo apt install -y docker.io docker-compose
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -a -G docker ubuntu
```

**⚠️ Importante**: Después de instalar Docker, reinicia la sesión SSH para aplicar permisos de grupo.

### 2. Instalar Docker Compose (si no está instalado)
```bash
sudo curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
```

### 3. Crear directorio de proyecto
```bash
mkdir -p ~/booky-backend
cd ~/booky-backend
```

### 4. Crear archivo .env
```bash
cat > .env << 'EOF'
# Database Configuration
POSTGRES_PASSWORD=secure_password_2024

# JWT Configuration
JWT_SECRET=booky_jwt_secret_key_very_secure_32_chars_minimum

# Cloudinary Configuration (opcional - reemplazar con tus valores)
CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

# Spring Profile
SPRING_PROFILES_ACTIVE=production

# Server Configuration
SERVER_PORT=8080
EOF
```

### 5. Crear docker-compose.yml
```bash
cat > docker-compose.yml << 'EOF'
services:
  postgres:
    image: postgres:15-alpine
    container_name: booky-postgres-prod
    environment:
      POSTGRES_DB: booky
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-admin}
      PGDATA: /var/lib/postgresql/data/pgdata
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - booky-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d booky"]
      interval: 30s
      timeout: 10s
      retries: 3

  booky-app:
    image: bookypfi/booky-backend:latest
    container_name: booky-backend-prod
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/booky
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: ${POSTGRES_PASSWORD:-admin}
      DATABASE_NAME: booky
      SPRING_PROFILES_ACTIVE: production
      SERVER_PORT: 8080
      JWT_SECRET: ${JWT_SECRET:-your-secret-key-here}
      CLOUDINARY_CLOUD_NAME: ${CLOUDINARY_CLOUD_NAME}
      CLOUDINARY_API_KEY: ${CLOUDINARY_API_KEY}
      CLOUDINARY_API_SECRET: ${CLOUDINARY_API_SECRET}
    ports:
      - "8080:8080"
    networks:
      - booky-network
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
    driver: local

networks:
  booky-network:
    driver: bridge
EOF
```

### 6. Desplegar la aplicación
```bash
# Hacer pull de la imagen
docker pull bookypfi/booky-backend:latest

# Iniciar servicios
docker-compose up -d

# Verificar estado
docker-compose ps

# Ver logs
docker-compose logs -f booky-app
```

## 🌐 Acceso a la aplicación

Una vez desplegada, tu aplicación estará disponible en:
- **URL**: `http://TU-IP-PUBLICA-EC2:8080`
- **Health Check**: `http://TU-IP-PUBLICA-EC2:8080/actuator/health`

## 📊 Comandos útiles

```bash
# Ver logs en tiempo real
docker-compose logs -f booky-app

# Detener servicios
docker-compose stop

# Iniciar servicios
docker-compose start

# Reiniciar servicios
docker-compose restart

# Detener y eliminar containers
docker-compose down

# Ver estado de containers
docker-compose ps

# Actualizar imagen
docker-compose pull
docker-compose up -d
```

## 🔧 Configuración adicional

### Variables de entorno importantes:
- `POSTGRES_PASSWORD`: Contraseña de la base de datos
- `JWT_SECRET`: Clave secreta para JWT (mínimo 32 caracteres)
- `CLOUDINARY_*`: Configuración de Cloudinary para imágenes

### Configuración de Security Groups:
Asegúrate de que tu EC2 tenga estos puertos abiertos:
- **22**: SSH
- **8080**: Aplicación web
- **5432**: PostgreSQL (opcional, solo si necesitas acceso externo)

## 🐞 Troubleshooting

### La aplicación no inicia:
```bash
# Verificar logs
docker-compose logs booky-app

# Verificar conectividad a la base de datos
docker-compose logs postgres
```

### Error de permisos de Docker:
```bash
# Verificar que el usuario esté en el grupo docker
groups

# Si no está en el grupo docker, reiniciar la sesión SSH
exit
ssh -i tu-key-pair.pem ec2-user@tu-ip-publica
```

### Problemas de conectividad:
```bash
# Verificar que los puertos estén abiertos
netstat -tlnp | grep :8080

# Verificar Security Groups en AWS Console
# Asegurarse de que el puerto 8080 esté abierto
```

## 📈 Monitoreo

### Health Check:
```bash
curl http://localhost:8080/actuator/health
```

### Logs del sistema:
```bash
# Logs de Docker
journalctl -u docker

# Logs de la aplicación
docker-compose logs --tail=100 booky-app
```

## 🔄 Actualización de la aplicación

Para actualizar la aplicación cuando haya una nueva versión:

```bash
# Hacer pull de la nueva imagen
docker-compose pull

# Recrear containers con la nueva imagen
docker-compose up -d

# Verificar que esté funcionando
docker-compose logs -f booky-app
``` 