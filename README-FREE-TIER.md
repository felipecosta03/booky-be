# Booky AWS Free Tier Deployment

Esta guía te ayudará a desplegar Booky en AWS utilizando los recursos del Free Tier.

## 🎯 Recursos Free Tier Incluidos

- **EC2 t2.micro**: 750 horas/mes (suficiente para 24/7)
- **RDS db.t3.micro**: 750 horas/mes con 20GB de almacenamiento
- **EBS**: 30GB de almacenamiento
- **Data Transfer**: 15GB salida/mes
- **Load Balancer**: 750 horas/mes

## 📋 Prerrequisitos

### 1. Cuenta AWS Real
- Cuenta AWS con tarjeta de crédito válida
- **NO** usar AWS Sandbox/Academy (limitaciones de recursos)
- Acceso a AWS Management Console

### 2. Herramientas Necesarias
- AWS CLI v2 instalado
- Java 11+ y Maven
- jq (para procesamiento JSON)
- PostgreSQL client (psql)

### 3. Configuración AWS CLI
```bash
# Instalar AWS CLI v2
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install

# Configurar credenciales
aws configure
```

Necesitarás:
- **Access Key ID**: De AWS Console → IAM → Users → Security credentials
- **Secret Access Key**: Generado junto con el Access Key
- **Region**: `us-east-1` (recomendado para Free Tier)
- **Output format**: `json`

## 🚀 Deployment Paso a Paso

### 1. Preparar el Proyecto
```bash
# Compilar la aplicación
mvn clean package -DskipTests

# Hacer ejecutables los scripts
chmod +x scripts/free-tier-deploy.sh
chmod +x scripts/manage-free-tier.sh
```

### 2. Ejecutar Deployment
```bash
# Desplegar en AWS Free Tier
./scripts/free-tier-deploy.sh
```

El script realizará automáticamente:
- ✅ Crear EC2 t2.micro instance
- ✅ Crear RDS PostgreSQL db.t3.micro
- ✅ Configurar Security Groups
- ✅ Instalar Docker y Docker Compose
- ✅ Desplegar la aplicación
- ✅ Inicializar base de datos con datos de prueba

### 3. Verificar Deployment
```bash
# Verificar estado
./scripts/manage-free-tier.sh status

# Ver logs
./scripts/manage-free-tier.sh logs

# Acceder via SSH
./scripts/manage-free-tier.sh ssh
```

## 🛠️ Comandos de Gestión

### Comandos Principales
```bash
# Estado del deployment
./scripts/manage-free-tier.sh status

# Verificar costos AWS
./scripts/manage-free-tier.sh costs

# Ver logs de aplicación
./scripts/manage-free-tier.sh logs

# Reiniciar aplicación
./scripts/manage-free-tier.sh restart

# Parar recursos (ahorra costos)
./scripts/manage-free-tier.sh stop

# Iniciar recursos
./scripts/manage-free-tier.sh start

# Monitoreo en tiempo real
./scripts/manage-free-tier.sh monitor

# Acceso SSH
./scripts/manage-free-tier.sh ssh

# Reinicializar base de datos
./scripts/manage-free-tier.sh init-db

# Limpiar recursos
./scripts/manage-free-tier.sh cleanup
```

### Comandos Avanzados
```bash
# Destruir deployment completo
./scripts/manage-free-tier.sh destroy

# Ayuda
./scripts/manage-free-tier.sh help
```

## 🔐 Usuarios de Prueba

La aplicación incluye usuarios preconfigurados:

### Administradores
- **admin@booky.com** - Rol: ADMIN
- **superadmin@booky.com** - Rol: SUPER_ADMIN

### Usuarios Regulares
- **user1@booky.com** hasta **user8@booky.com** - Rol: USER

**Contraseña para todos**: `password123`

## 📊 Monitoreo de Costos

### Verificar Uso Free Tier
```bash
# Ver costos actuales
./scripts/manage-free-tier.sh costs

# Monitorear recursos
./scripts/manage-free-tier.sh monitor
```

### Límites Free Tier
- **EC2 t2.micro**: 750 horas/mes
- **RDS db.t3.micro**: 750 horas/mes
- **Storage**: 30GB EBS + 20GB RDS
- **Data Transfer**: 15GB salida/mes

### Optimizaciones Implementadas
- **JVM**: `-Xms256m -Xmx512m` (optimizado para 1GB RAM)
- **Swap**: 1GB configurado automáticamente
- **Nginx**: Reverse proxy eficiente
- **Docker**: Límites de memoria y CPU
- **Postgres**: Configuración optimizada

## 🔧 Configuración Avanzada

### Variables de Entorno
El archivo `env.free-tier` contiene:
```bash
# Base de datos
DB_HOST=<rds-endpoint>
DB_PORT=5432
DB_NAME=booky
DB_USERNAME=booky
DB_PASSWORD=BookyPassword123!

# Aplicación
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production

# Optimizaciones Free Tier
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
```

### Estructura de Archivos
```
scripts/
├── free-tier-deploy.sh          # Script principal de deployment
├── manage-free-tier.sh          # Script de gestión
├── init-database-rds.sql        # Inicialización automática de BD
└── verify-setup.sh              # Verificación pre-deployment

docker-compose.free-tier.yml      # Configuración Docker optimizada
nginx.conf                        # Configuración Nginx
env.free-tier                     # Variables de entorno
```

## 🚨 Troubleshooting

### Errores Comunes

#### 1. "InvalidClientTokenId"
```bash
# Verificar credenciales
aws sts get-caller-identity

# Reconfigurar si es necesario
aws configure
```

#### 2. "Instance limit exceeded"
```bash
# Verificar límites Free Tier
aws ec2 describe-account-attributes --attribute-names supported-platforms

# Terminar instancias no utilizadas
aws ec2 terminate-instances --instance-ids i-xxxxxxxxx
```

#### 3. "RDS instance creation failed"
```bash
# Verificar cuota RDS
aws rds describe-account-attributes

# Borrar instancias RDS no utilizadas
aws rds delete-db-instance --db-instance-identifier old-instance --skip-final-snapshot
```

#### 4. "Application not responding"
```bash
# Verificar logs
./scripts/manage-free-tier.sh logs

# Reiniciar aplicación
./scripts/manage-free-tier.sh restart

# Verificar recursos del sistema
./scripts/manage-free-tier.sh ssh
# Dentro de la instancia:
free -h
df -h
docker stats
```

### Logs Útiles
```bash
# Logs de aplicación
./scripts/manage-free-tier.sh logs

# Logs de sistema (SSH)
./scripts/manage-free-tier.sh ssh
tail -f /var/log/cloud-init-output.log
journalctl -u docker
```

## 💡 Mejores Prácticas

### 1. Gestión de Costos
- Parar instancias cuando no las uses: `./scripts/manage-free-tier.sh stop`
- Monitorear uso mensual: `./scripts/manage-free-tier.sh costs`
- Eliminar resources no utilizados: `./scripts/manage-free-tier.sh cleanup`

### 2. Seguridad
- Cambiar contraseñas por defecto
- Usar HTTPS en producción
- Configurar backup de base de datos
- Actualizar JWT_SECRET

### 3. Performance
- Monitorear recursos: `./scripts/manage-free-tier.sh monitor`
- Optimizar consultas de base de datos
- Usar caché cuando sea posible
- Configurar CloudWatch alarms

## 🔄 Actualización y Mantenimiento

### Actualizar Aplicación
```bash
# Compilar nueva versión
mvn clean package -DskipTests

# Redesplegar
./scripts/free-tier-deploy.sh
```

### Backup Base de Datos
```bash
# Crear backup
./scripts/manage-free-tier.sh ssh
# Dentro de la instancia:
pg_dump -h $DB_HOST -U $DB_USERNAME -d $DB_NAME > backup.sql
```

### Restaurar Base de Datos
```bash
# Reinicializar con datos de prueba
./scripts/manage-free-tier.sh init-db

# Restaurar desde backup
./scripts/manage-free-tier.sh ssh
# Dentro de la instancia:
psql -h $DB_HOST -U $DB_USERNAME -d $DB_NAME < backup.sql
```

## 📞 Soporte

### Recursos Útiles
- [AWS Free Tier FAQ](https://aws.amazon.com/free/faqs/)
- [AWS Free Tier Usage](https://console.aws.amazon.com/billing/home#/freetier)
- [AWS Support](https://console.aws.amazon.com/support/home)

### Comandos de Diagnóstico
```bash
# Verificar estado completo
./scripts/manage-free-tier.sh status

# Ver todos los recursos
aws ec2 describe-instances
aws rds describe-db-instances

# Verificar facturas
aws ce get-cost-and-usage --time-period Start=2024-01-01,End=2024-01-31 --granularity MONTHLY --metrics BlendedCost
```

---

**¡Listo para usar!** 🎉

Tu aplicación Booky estará disponible en `http://EC2-PUBLIC-IP` una vez completado el deployment.

Para cualquier problema, revisa los logs con `./scripts/manage-free-tier.sh logs` o conecta via SSH con `./scripts/manage-free-tier.sh ssh`. 