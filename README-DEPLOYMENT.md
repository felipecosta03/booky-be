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

#### 2. **Deployment Rápido** (Para instancias existentes)
```bash
# Script optimizado para instancias ya creadas
./scripts/quick-deploy.sh
```

#### 3. **Deployment Simplificado** (Para casos especiales)
```bash
# Para casos donde los otros scripts fallan
./scripts/simple-deploy.sh
```

#### 4. **Deployment Completamente Manual**
Para casos extremos, ver [TROUBLESHOOTING.md](./TROUBLESHOOTING.md)

### 🔧 Errores Comunes y Soluciones

| Error | Solución |
|-------|----------|
| `Instances not in a valid state for account` | Usar `./scripts/quick-deploy.sh` o `./scripts/simple-deploy.sh` |
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