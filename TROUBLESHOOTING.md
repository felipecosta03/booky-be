# 🔧 Troubleshooting Guide - Booky Backend Deployment

## 🚨 Problema: "Instances not in a valid state for account"

### ¿Qué significa este error?
Este error ocurre cuando tratamos de usar AWS Systems Manager (SSM) para conectarnos a una instancia EC2 que no está configurada para SSM o cuando la instancia no tiene los permisos necesarios.

### ¿Por qué sucede?
- **AWS Sandbox**: Las instancias creadas en AWS Sandbox no siempre tienen el rol IAM necesario para SSM
- **Instancia existente**: Si la instancia ya existía, puede no tener SSM habilitado
- **Permisos limitados**: AWS Sandbox tiene restricciones en la creación de roles IAM

## 🔄 Soluciones

### Solución 1: Usar el Script Simplificado (Recomendado)
```bash
# Ejecutar el script simplificado
chmod +x scripts/simple-deploy.sh
./scripts/simple-deploy.sh
```

### Solución 2: Deployment Manual
Si el script automático falla, sigue estos pasos:

1. **Conéctate a la instancia** usando la consola AWS EC2:
   - Ve a EC2 → Instances
   - Selecciona tu instancia `booky-server`
   - Click "Connect" → "EC2 Instance Connect"

2. **Ejecuta estos comandos en la instancia**:
   ```bash
   # Actualizar sistema
   sudo apt update
   
   # Crear directorio para la aplicación
   sudo mkdir -p /opt/booky-app/target
   sudo chown -R ubuntu:ubuntu /opt/booky-app
   
   # Parar contenedores existentes (si los hay)
   cd /opt/booky-app && docker-compose -f docker-compose.prod.yml down 2>/dev/null || true
   ```

3. **Subir archivos** usando la consola AWS:
   - En tu computadora local, ejecuta: `mvn clean package -DskipTests`
   - Ejecuta: `./scripts/simple-deploy.sh`
   - Esto creará los archivos necesarios
   - Usa SCP o la consola AWS para subir:
     - `docker-compose.prod.yml` → `/opt/booky-app/`
     - `target/booky-be-0.0.1-SNAPSHOT.jar` → `/opt/booky-app/target/`

4. **Ejecutar deployment en la instancia**:
   ```bash
   cd /opt/booky-app
   docker-compose -f docker-compose.prod.yml up -d
   ```

### Solución 3: Recrear la Instancia
Si nada más funciona, recrear la instancia:

```bash
# Terminar instancia existente
aws ec2 terminate-instances --instance-ids i-xxxxxxxxx

# Ejecutar setup completo
./scripts/setup-and-deploy.sh
```

## 🔍 Verificar el Deployment

Una vez completado el deployment:

```bash
# Verificar estado de contenedores
docker ps

# Verificar logs de la aplicación
docker logs booky-backend

# Verificar health check
curl http://localhost:8080/actuator/health
```

## 📊 URLs de la Aplicación

Después del deployment exitoso:
- **Aplicación**: `http://YOUR_EC2_IP/`
- **Health Check**: `http://YOUR_EC2_IP/actuator/health`
- **API Docs**: `http://YOUR_EC2_IP/swagger-ui.html`
- **Database Admin**: `http://YOUR_EC2_IP:8081` (Adminer)

## 🎯 Problemas Específicos

### SSH Key Issues
```bash
# Error: "ssh-keyscan failed"
# Solución: El script creará automáticamente nuevas keys

# Error: "Permission denied (publickey)"
# Solución: Usar EC2 Instance Connect desde la consola AWS
```

### Database Connection Issues
```bash
# Error: "Connection refused"
# Solución: Verificar que PostgreSQL esté corriendo
docker logs booky-postgres

# Error: "database does not exist"
# Solución: Verificar variables de entorno
docker exec booky-postgres psql -U postgres -l
```

### Application Startup Issues
```bash
# Error: "Port 8080 already in use"
# Solución: Parar contenedores existentes
docker-compose -f docker-compose.prod.yml down

# Error: "JAR not found"
# Solución: Verificar que el JAR está en el directorio correcto
ls -la /opt/booky-app/target/
```

## 🔧 Scripts de Ayuda

### Reiniciar la Aplicación
```bash
cd /opt/booky-app
docker-compose -f docker-compose.prod.yml restart
```

### Ver Logs en Tiempo Real
```bash
cd /opt/booky-app
docker-compose -f docker-compose.prod.yml logs -f
```

### Limpiar y Rebuildar
```bash
cd /opt/booky-app
docker-compose -f docker-compose.prod.yml down
docker system prune -f
docker-compose -f docker-compose.prod.yml up -d
```

## 📞 ¿Necesitas más ayuda?

1. **Revisa los logs** de GitHub Actions para ver el error específico
2. **Conecta directamente** a la instancia EC2 via consola AWS
3. **Verifica las variables de entorno** en GitHub Secrets
4. **Confirma que tu AWS Sandbox** esté activo y las credenciales no hayan expirado

---

💡 **Tip**: Para AWS Sandbox, es más confiable usar el deployment manual la primera vez, y luego usar el script simplificado para actualizaciones. 