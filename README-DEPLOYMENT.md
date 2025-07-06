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

### Si usas AWS Sandbox:
- Las credenciales expiran → Ejecuta `./setup-aws-sandbox.sh` nuevamente
- Lab cerrado → Inicia el lab y actualiza credenciales

### Si usas AWS Regular:
- Revisa que los secrets estén bien configurados
- Verifica permisos de AWS IAM

## 📞 ¿Necesitas ayuda?

1. **Revisa los logs** en GitHub Actions
2. **Conecta por SSH** y revisa logs: `./scripts/manage-server.sh logs`
3. **Verifica health check**: `http://TU_IP/actuator/health`

## 🎯 Archivos Importantes

- `setup-aws-sandbox.sh` - Setup para AWS Sandbox
- `setup-aws-deployment.sh` - Setup para AWS Regular
- `scripts/manage-server.sh` - Gestión del servidor
- `DEPLOYMENT.md` - Documentación completa

## 🎉 ¡Eso es todo!

Con estos scripts, tu aplicación estará en producción en minutos. ¿Fácil, no?

---

**¿Primera vez?** → Usa `./setup-aws-sandbox.sh` 
**¿Experto?** → Usa `./setup-aws-deployment.sh`
**¿Problemas?** → Revisa `DEPLOYMENT.md` 