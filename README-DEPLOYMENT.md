# 🚀 Deployment Rápido a AWS EC2

¿Quieres desplegar tu aplicación Booky Backend en AWS EC2 de forma automática? ¡Estás en el lugar correcto!

## 🎯 Opción más Sencilla: AWS Sandbox

Si tienes **AWS Learner Lab** o un **AWS Sandbox**, usa este comando:

```bash
./setup-aws-sandbox.sh
```

**¿Por qué esta opción?**
- ✅ **Gratis** para estudiantes
- ✅ **Súper fácil** de configurar
- ✅ **Funciona en 5 minutos**

## 🔧 Opción Avanzada: AWS Cuenta Regular

Si tienes una cuenta AWS regular:

```bash
./setup-aws-deployment.sh
```

## 📋 ¿Qué necesitas antes de empezar?

### Para AWS Sandbox:
1. **AWS Learner Lab** funcionando
2. **Repository en GitHub** con tu código
3. **5 minutos** de tu tiempo

### Para AWS Cuenta Regular:
1. **Cuenta AWS** con permisos
2. **Repository en GitHub** con tu código
3. **AWS CLI** instalado (opcional)

## 🚀 Proceso Paso a Paso

### 1. Ejecutar el script
```bash
# Para AWS Sandbox
./setup-aws-sandbox.sh

# Para AWS Regular
./setup-aws-deployment.sh
```

### 2. Seguir las instrucciones
El script te pedirá:
- Credenciales de AWS (te explica cómo obtenerlas)
- Configuración de Cloudinary (opcional)
- Te genera todos los secrets automáticamente

### 3. Configurar GitHub Secrets
El script te da **exactamente** los valores que necesitas copiar en:
`Repository → Settings → Secrets and variables → Actions`

### 4. Hacer Push
```bash
git add .
git commit -m "Setup AWS deployment"
git push origin main
```

### 5. ¡Listo!
Tu aplicación se desplegará automáticamente. Ve a **GitHub Actions** para ver el progreso.

## 🌐 Acceso a tu Aplicación

Cuando termine el deployment, encontrarás en los logs:
- **IP Pública**: `http://TU_IP_PUBLICA`
- **Health Check**: `http://TU_IP_PUBLICA/actuator/health`
- **API Docs**: `http://TU_IP_PUBLICA/swagger-ui.html`

## 🛠️ Gestión del Servidor

Una vez desplegado:

```bash
# Conectarse al servidor
./scripts/manage-server.sh connect

# Ver logs
./scripts/manage-server.sh logs

# Ver estado
./scripts/manage-server.sh status

# Reiniciar
./scripts/manage-server.sh restart
```

## 💰 Costos

- **AWS Sandbox**: **GRATIS** ✅
- **AWS Regular t3.medium**: ~$38/mes
- **AWS Regular t3.small**: ~$23/mes

## 🔄 Actualizaciones Automáticas

Cada push a `main` actualiza automáticamente tu aplicación:
1. ✅ Construye la aplicación
2. ✅ Crea nueva imagen Docker
3. ✅ Despliega sin downtime
4. ✅ Verifica que funcione

## 🚨 Troubleshooting

### ⚡ Opciones de Deployment

#### 1. **Deployment Automático** (Recomendado)
```bash
# Push a main activa GitHub Actions automáticamente
git push origin main
```

#### 2. **Deployment para AWS Sandbox** 🎓 (Recomendado para Learner Lab)
```bash
# Script especialmente optimizado para AWS Learner Lab
./scripts/sandbox-deploy.sh
```

#### 3. **Deployment Rápido** (Para instancias existentes)
```bash
# Script optimizado para instancias ya creadas
./scripts/quick-deploy.sh
```

#### 4. **Deployment Simplificado** (Para casos especiales)
```bash
# Para casos donde los otros scripts fallan
./scripts/simple-deploy.sh
```

#### 5. **Deployment Completamente Manual**
Para casos extremos, ver [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)

### 🔧 Errores Comunes y Soluciones

| Error | Solución |
|-------|----------|
| `AccessDenied` al crear roles IAM | **Usar `./scripts/sandbox-deploy.sh` para AWS Learner Lab** 🎓 |
| `Instances not in a valid state for account` | Usar `./scripts/sandbox-deploy.sh` o `./scripts/quick-deploy.sh` |
| `ssh-keyscan failed` | Usar `./scripts/quick-deploy.sh` (encuentra keys automáticamente) |
| `JAR not found` | Ejecutar `mvn clean package -DskipTests` primero |
| `Health check failed` | La app puede tardar en iniciar, verificar manualmente |
| `Connection timed out` | Usar `./scripts/quick-deploy.sh` con keys existentes |

### 🔄 AWS Sandbox Específico

| Problema | Solución |
|----------|----------|
| **Credenciales expiran** | Ejecutar `./setup-aws-sandbox.sh` nuevamente |
| **Lab cerrado** | Iniciar el lab y actualizar credenciales |
| **Error SSH/SSM** | Usar el script simplificado o deployment manual |

### 📚 Guía Detallada

Para troubleshooting completo, consulta: **[TROUBLESHOOTING.md](./TROUBLESHOOTING.md)**

## 📞 ¿Necesitas ayuda?

1. **Revisa los logs** de GitHub Actions para ver errores específicos
2. **Consulta** [TROUBLESHOOTING.md](./TROUBLESHOOTING.md) para soluciones detalladas
3. **Prueba** el script simplificado: `./scripts/simple-deploy.sh`
4. **Conecta por SSH** y revisa logs: `./scripts/manage-server.sh logs`

## 🎯 Archivos Importantes

### 📋 Scripts de Configuración:
- `setup-aws-sandbox.sh` - Setup para AWS Sandbox
- `setup-aws-deployment.sh` - Setup para AWS Regular

### 🚀 Scripts de Deployment:
- `scripts/setup-and-deploy.sh` - Script principal (crea instancia + deploy)
- `scripts/sandbox-deploy.sh` - **Script optimizado para AWS Learner Lab** 🎓
- `scripts/quick-deploy.sh` - Script rápido (para instancias existentes)
- `scripts/simple-deploy.sh` - Script simplificado (para troubleshooting)
- `scripts/manage-server.sh` - Gestión del servidor

### 📚 Documentación:
- `DEPLOYMENT.md` - Documentación completa
- `TROUBLESHOOTING.md` - Guía de solución de problemas

## 🎉 ¡Eso es todo!

Con estos scripts, tu aplicación estará en producción en minutos. ¿Fácil, no?

---

**¿Primera vez?** → Usa `./setup-aws-sandbox.sh` 
**¿Experto?** → Usa `./setup-aws-deployment.sh`
**¿Problemas?** → Revisa `DEPLOYMENT.md`

# 🚀 Guía de Deployment - Booky Backend

## 📋 Resumen

Tu instancia EC2 **ya está creada** y funcionando! 🎉

- **Instancia**: `i-0c84eb49ef31ffac7`
- **IP Pública**: `54.174.40.56`
- **SSH Key**: `booky-sandbox-key.pem`

El script creó todo correctamente, solo necesitaba más tiempo para que SSH estuviera listo.

## 🔧 Opciones de Deployment

### ✅ Opción 1: Reconectar y completar (RECOMENDADO)
```bash
./scripts/reconnect-deploy.sh
```
Este script:
- Se conecta a tu instancia existente
- Espera pacientemente a que SSH esté listo
- Completa el deployment automáticamente

### 🆕 Opción 2: Crear nueva instancia
```bash
./scripts/sandbox-deploy.sh
```
Solo si quieres crear una nueva instancia desde cero.

### ⚡ Opción 3: Deployment vía GitHub Actions
```bash
git add .
git commit -m "Deploy to production"
git push origin main
```

## 🔍 Verificación Manual

Si quieres verificar que tu instancia está funcionando:

```bash
# Verificar que la instancia existe
aws ec2 describe-instances --instance-ids i-0c84eb49ef31ffac7

# Probar SSH manualmente (puede tomar 5-10 minutos)
ssh -i booky-sandbox-key.pem ubuntu@54.174.40.56
```

## 🔧 Troubleshooting

### SSH tarda en conectarse
**Es normal**. La instancia está:
- Instalando Docker
- Configurando Nginx
- Configurando dependencias
- Puede tomar 5-10 minutos

### ¿Qué hacer si SSH no funciona?
1. **Esperar más tiempo** (lo más común)
2. **Usar AWS Console**:
   - Ve a AWS Console → EC2 → Instances
   - Selecciona `i-0c84eb49ef31ffac7`
   - Click "Connect" → "EC2 Instance Connect"

### Verificar logs de la instancia
En AWS Console:
- Ve a EC2 → Instances
- Selecciona tu instancia
- Actions → Monitor and troubleshoot → Get system log

## 🎯 URLs de tu aplicación

Una vez que el deployment esté completo:
- **Aplicación**: http://54.174.40.56
- **API Health**: http://54.174.40.56/actuator/health
- **Swagger UI**: http://54.174.40.56/swagger-ui.html

## 🔑 Variables de entorno requeridas

Para GitHub Actions, configura estos secrets:
```
AWS_ACCESS_KEY_ID=tu_access_key
AWS_SECRET_ACCESS_KEY=tu_secret_key
AWS_SESSION_TOKEN=tu_session_token
DATABASE_PASSWORD=tu_password_db
JWT_SECRET=tu_jwt_secret
CLOUDINARY_CLOUD_NAME=tu_cloud_name
CLOUDINARY_API_KEY=tu_api_key
CLOUDINARY_API_SECRET=tu_api_secret
```

## 📝 Comandos útiles

```bash
# Ver estado de la instancia
aws ec2 describe-instances --instance-ids i-0c84eb49ef31ffac7

# Conectarse por SSH
ssh -i booky-sandbox-key.pem ubuntu@54.174.40.56

# Ver logs de la aplicación (una vez conectado)
cd /opt/booky-app && docker-compose -f docker-compose.prod.yml logs

# Reiniciar aplicación
cd /opt/booky-app && docker-compose -f docker-compose.prod.yml restart

# Ver contenedores
docker ps
```

## 🎉 ¡Listo!

El deployment está configurado. Solo ejecuta:
```bash
./scripts/reconnect-deploy.sh
```

Y tendrás tu aplicación corriendo en AWS! 🚀 