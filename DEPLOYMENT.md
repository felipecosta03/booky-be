# 🚀 Deployment Automático a EC2 con Docker Compose

Esta guía te ayudará a configurar un deployment automático de tu aplicación Booky Backend en una instancia EC2 de AWS usando Docker Compose.

## 📋 Requisitos Previos

1. **Cuenta de AWS** con permisos para crear EC2 instances
2. **Repository en GitHub** con tu código
3. **AWS CLI** configurado (opcional, para testing local)

## 🎓 AWS Sandbox / Learner Lab

Si estás usando **AWS Learner Lab** o un **AWS Sandbox**, tienes credenciales temporales que incluyen un **session token**. Para este caso:

### ✅ Ventajas del AWS Sandbox
- **Gratis** para estudiantes
- **Fácil de usar** 
- **Ideal para aprender** y hacer pruebas

### ⚠️ Limitaciones del AWS Sandbox
- **Credenciales temporales** (expiran cuando cierras el lab)
- **Recursos limitados** (pero suficientes para esta aplicación)
- **Región fija** (generalmente us-east-1)

### 🔑 Cómo obtener credenciales de AWS Sandbox
1. Abre **AWS Learner Lab**
2. Haz clic en **"Start Lab"** (espera que se ponga verde)
3. Haz clic en **"AWS Details"**
4. Haz clic en **"AWS CLI"**
5. Copia las credenciales que aparecen (incluye session token)

### 🚀 Setup rápido para AWS Sandbox
```bash
./setup-aws-sandbox.sh
```

Este script te guiará paso a paso y es **mucho más sencillo** que el setup regular.

## 🔧 Configuración Inicial

### Para AWS Sandbox (Recomendado para principiantes)
```bash
# Ejecuta este script y sigue las instrucciones
./setup-aws-sandbox.sh
```

### Para AWS Cuenta Regular (Avanzado)
```bash
# Ejecuta este script para configuración completa
./setup-aws-deployment.sh
```

### Configuración Manual (Solo si necesitas personalizar)

#### 1. Configurar AWS Credentials

**AWS Sandbox:**
1. Ve a AWS Learner Lab → AWS Details → AWS CLI
2. Copia las credenciales (incluye session token)

**AWS Regular:**
1. Ve a AWS IAM → Users → Tu usuario → Security credentials
2. Crea un nuevo Access Key
3. Guarda el Access Key ID y Secret Access Key

#### 2. Configurar GitHub Secrets

Ve a tu repositorio en GitHub → Settings → Secrets and variables → Actions:

**AWS Sandbox:**
```
AWS_ACCESS_KEY_ID: tu_access_key_id
AWS_SECRET_ACCESS_KEY: tu_secret_access_key
AWS_SESSION_TOKEN: tu_session_token
AWS_REGION: us-east-1
```

**AWS Regular:**
```
AWS_ACCESS_KEY_ID: tu_access_key_id
AWS_SECRET_ACCESS_KEY: tu_secret_access_key
AWS_REGION: us-east-1 (o tu región preferida)
```

**Application Configuration (ambos casos):**
```
DATABASE_PASSWORD: tu_password_seguro_para_postgresql
JWT_SECRET: tu_jwt_secret_de_al_menos_32_caracteres
CLOUDINARY_CLOUD_NAME: tu_cloudinary_cloud_name
CLOUDINARY_API_KEY: tu_cloudinary_api_key
CLOUDINARY_API_SECRET: tu_cloudinary_api_secret
```

## 🚀 Primer Deployment

### Opción 1: AWS Sandbox (Learner Lab) - Recomendado para principiantes
```bash
# Usa este script simplificado para AWS Sandbox
./setup-aws-sandbox.sh
```

### Opción 2: AWS Cuenta Regular
```bash
# Usa este script para cuenta AWS regular
./setup-aws-deployment.sh
```

### Opción 3: Manual
1. Ve a Actions en tu repositorio de GitHub
2. Selecciona "Deploy to EC2"
3. Haz clic en "Run workflow"

## 📊 ¿Qué hace el deployment?

### 1. Setup de EC2 (`setup-ec2.sh`)
- ✅ Crea una instancia EC2 (t3.medium por defecto)
- ✅ Configura Security Groups (puertos 22, 80, 443, 8080)
- ✅ Instala Docker y Docker Compose
- ✅ Configura Nginx como reverse proxy
- ✅ Crea Key Pair para SSH

### 2. Deployment (`deploy.sh`)
- ✅ Copia archivos al servidor
- ✅ Configura variables de entorno
- ✅ Ejecuta Docker Compose
- ✅ Verifica que la aplicación esté funcionando

## 🔧 Configuración Personalizada

### Cambiar el tipo de instancia EC2
En el archivo `scripts/setup-ec2.sh`, modifica:
```bash
INSTANCE_TYPE=${EC2_INSTANCE_TYPE:-t3.medium}
```

### Cambiar la región de AWS
En GitHub Secrets, modifica `AWS_REGION` al valor deseado.

### Personalizar el Security Group
Modifica el archivo `scripts/setup-ec2.sh` para añadir más puertos si necesitas.

## 🌐 Acceso a la Aplicación

Después del primer deployment, encontrarás en los logs:

```
🌐 IP Pública: X.X.X.X
📋 Configuración completada!

Para conectarte por SSH:
ssh -i booky-key.pem ubuntu@X.X.X.X
```

### URLs Disponibles
- **Aplicación**: `http://TU_IP_PUBLICA`
- **API Health**: `http://TU_IP_PUBLICA/actuator/health`
- **API Docs**: `http://TU_IP_PUBLICA/swagger-ui.html`
- **Aplicación directa**: `http://TU_IP_PUBLICA:8080`

## 🐳 Gestión de Contenedores

### Conectarse al servidor
```bash
ssh -i booky-key.pem ubuntu@TU_IP_PUBLICA
```

### Ver logs de la aplicación
```bash
cd /opt/booky-app
docker-compose -f docker-compose.prod.yml logs -f booky-app
```

### Ver estado de contenedores
```bash
cd /opt/booky-app
docker-compose -f docker-compose.prod.yml ps
```

### Reiniciar la aplicación
```bash
cd /opt/booky-app
docker-compose -f docker-compose.prod.yml restart booky-app
```

## 🔒 Seguridad

### Recomendaciones de Seguridad

1. **Cambia las credenciales por defecto**
   - El password de PostgreSQL se configura desde GitHub Secrets
   - El JWT Secret debe ser único y seguro

2. **Configura HTTPS** (opcional pero recomendado)
   - Usa Let's Encrypt para SSL certificates
   - Configura un dominio personalizado

3. **Backup de la base de datos**
   - Los datos se almacenan en volumes de Docker
   - Considera backups regulares a S3

### Configurar HTTPS con Let's Encrypt (opcional)

```bash
# Conectarse al servidor
ssh -i booky-key.pem ubuntu@TU_IP_PUBLICA

# Instalar certbot
sudo apt install certbot python3-certbot-nginx

# Configurar tu dominio (reemplaza con tu dominio)
sudo certbot --nginx -d tu-dominio.com

# El certificado se renovará automáticamente
```

## 🚨 Troubleshooting

### La aplicación no responde
```bash
# Verificar logs
docker-compose -f docker-compose.prod.yml logs -f

# Verificar estado de contenedores
docker-compose -f docker-compose.prod.yml ps

# Reiniciar todo
docker-compose -f docker-compose.prod.yml down
docker-compose -f docker-compose.prod.yml up -d
```

### Problemas de conexión SSH
```bash
# Verificar que el archivo key tenga los permisos correctos
chmod 600 booky-key.pem

# Verificar que el Security Group permita SSH (puerto 22)
```

### Base de datos no se conecta
```bash
# Verificar que PostgreSQL esté corriendo
docker-compose -f docker-compose.prod.yml ps postgres

# Ver logs de PostgreSQL
docker-compose -f docker-compose.prod.yml logs postgres
```

## 💰 Costos Estimados

### Instancia t3.medium (recomendada)
- **Compute**: ~$30/mes
- **Storage**: ~$8/mes (80GB GP3)
- **Network**: Minimal
- **Total**: ~$38/mes

### Instancia t3.small (mínima)
- **Compute**: ~$15/mes
- **Storage**: ~$8/mes (80GB GP3)
- **Network**: Minimal
- **Total**: ~$23/mes

## 🔄 Actualizaciones

### Actualizaciones Automáticas
Cada push a `main` o `master` actualizará automáticamente la aplicación:

1. Se ejecutan las pruebas
2. Se construye la nueva imagen
3. Se despliega sin downtime
4. Se verifica que la aplicación funcione

### Rollback Manual
Si necesitas hacer rollback:

```bash
# Conectarse al servidor
ssh -i booky-key.pem ubuntu@TU_IP_PUBLICA

# Ver imágenes disponibles
docker images

# Cambiar a una imagen anterior
docker-compose -f docker-compose.prod.yml down
# Editar docker-compose.prod.yml para usar la imagen anterior
docker-compose -f docker-compose.prod.yml up -d
```

## 📞 Soporte

Si tienes problemas:

1. **Revisa los logs** del GitHub Actions workflow
2. **Verifica que todos los secrets** estén configurados correctamente
3. **Conecta por SSH** y revisa los logs de Docker
4. **Verifica los Security Groups** en AWS

## 🎯 Próximos Pasos

1. **Configurar un dominio personalizado**
2. **Añadir SSL/HTTPS**
3. **Configurar backups automáticos**
4. **Añadir monitoring con CloudWatch**
5. **Configurar alertas**

¡Tu aplicación Booky Backend está lista para producción! 🎉 