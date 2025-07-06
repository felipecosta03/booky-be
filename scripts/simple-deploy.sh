#!/bin/bash

# Script simplificado para deployment en AWS Sandbox
# Maneja casos donde ya existe una instancia EC2 sin complicaciones de SSM

set -e

# Configuración
REGION=${AWS_REGION:-us-east-1}
INSTANCE_NAME=${EC2_INSTANCE_NAME:-booky-server}
KEY_NAME=${EC2_KEY_NAME:-booky-key}

echo "🚀 Deployment simplificado para AWS Sandbox..."

# Verificar que el JAR existe
JAR_FILE=$(find target -name "*.jar" -not -name "*-sources.jar" | head -1)
if [[ ! -f "$JAR_FILE" ]]; then
  echo "❌ Error: No se encontró el archivo JAR en target/"
  echo "Ejecuta 'mvn clean package -DskipTests' primero"
  exit 1
fi

JAR_NAME=$(basename "$JAR_FILE")
echo "✅ JAR encontrado: $JAR_NAME"

# Buscar instancia existente
echo "📝 Buscando instancia EC2 existente..."
INSTANCE_ID=$(aws ec2 describe-instances \
  --region $REGION \
  --filters "Name=tag:Name,Values=$INSTANCE_NAME" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text 2>/dev/null || echo "None")

if [[ "$INSTANCE_ID" == "None" || "$INSTANCE_ID" == "" ]]; then
  echo "❌ No se encontró instancia existente"
  echo "💡 Ejecuta primero el script principal: ./scripts/setup-and-deploy.sh"
  exit 1
fi

# Obtener IP pública
PUBLIC_IP=$(aws ec2 describe-instances \
  --region $REGION \
  --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo "✅ Instancia encontrada: $INSTANCE_ID"
echo "🌐 IP Pública: $PUBLIC_IP"

# Crear nueva key pair para acceso
TEMP_KEY_NAME="booky-deploy-$(date +%s)"
echo "🔑 Creando key pair temporal para deployment..."

aws ec2 create-key-pair --region $REGION --key-name $TEMP_KEY_NAME --query 'KeyMaterial' --output text > ${TEMP_KEY_NAME}.pem
chmod 400 ${TEMP_KEY_NAME}.pem

# Obtener la public key
PUBLIC_KEY=$(ssh-keygen -y -f ${TEMP_KEY_NAME}.pem)

echo "🔧 Agregando key a la instancia..."
echo "💡 Esto requiere que tengas acceso a la instancia por otro método (consola AWS, key existente, etc.)"

# Crear script de deployment que se puede ejecutar manualmente
cat > deploy-commands.sh << EOF
#!/bin/bash
# Comandos para ejecutar en la instancia EC2

# Agregar nueva SSH key
echo "$PUBLIC_KEY" >> ~/.ssh/authorized_keys

# Crear directorio de la aplicación
sudo mkdir -p /opt/booky-app/target
sudo chown -R ubuntu:ubuntu /opt/booky-app

# Parar contenedores existentes
cd /opt/booky-app && docker-compose -f docker-compose.prod.yml down 2>/dev/null || true
EOF

echo "📝 Script de comandos creado: deploy-commands.sh"

# Crear docker-compose de producción
cat > docker-compose.prod.yml << EOF
services:
  postgres:
    image: postgres:15-alpine
    container_name: booky-postgres
    environment:
      POSTGRES_DB: booky
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${DATABASE_PASSWORD}
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
    image: openjdk:17-jdk-slim
    container_name: booky-backend
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/booky
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: ${DATABASE_PASSWORD}
      DATABASE_NAME: booky
      DDL_AUTO: update
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRATION: 86400000
      CLOUDINARY_CLOUD_NAME: ${CLOUDINARY_CLOUD_NAME}
      CLOUDINARY_API_KEY: ${CLOUDINARY_API_KEY}
      CLOUDINARY_API_SECRET: ${CLOUDINARY_API_SECRET}
      CORS_ALLOWED_ORIGINS: http://localhost:3000,http://${PUBLIC_IP}
      CORS_ALLOWED_METHODS: GET,POST,PUT,DELETE,OPTIONS
      CORS_ALLOWED_HEADERS: "*"
      CORS_ALLOW_CREDENTIALS: true
      LOG_LEVEL: INFO
      APP_LOG_LEVEL: INFO
      SECURITY_LOG_LEVEL: INFO
      SHOW_SQL: false
      FORMAT_SQL: false
      SPRING_PROFILES_ACTIVE: prod
      OPENAPI_PROD_URL: http://${PUBLIC_IP}
    ports:
      - "8080:8080"
    volumes:
      - ./target:/app
    working_dir: /app
    command: ["java", "-jar", "${JAR_NAME}"]
    networks:
      - booky-network
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

volumes:
  postgres_data:
    driver: local

networks:
  booky-network:
    driver: bridge
EOF

# Intentar deployment automático
echo "🚀 Intentando deployment automático..."

# Probar si podemos conectar con alguna key existente
SSH_SUCCESS=false

# Intentar con keys existentes
for key_file in *.pem; do
  if [[ -f "$key_file" ]]; then
    echo "🔍 Probando con key: $key_file"
    if ssh -i "$key_file" -o StrictHostKeyChecking=no -o ConnectTimeout=5 ubuntu@$PUBLIC_IP "echo 'Conexión exitosa'" 2>/dev/null; then
      echo "✅ Conexión exitosa con $key_file"
      SSH_SUCCESS=true
      
      # Agregar nueva key
      echo "🔑 Agregando nueva key SSH..."
      ssh -i "$key_file" -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "echo '$PUBLIC_KEY' >> ~/.ssh/authorized_keys"
      
      # Ahora usar la nueva key
      cp "${TEMP_KEY_NAME}.pem" ~/.ssh/booky_deploy_key
      chmod 600 ~/.ssh/booky_deploy_key
      
      echo "📦 Copiando archivos..."
      scp -i ~/.ssh/booky_deploy_key -o StrictHostKeyChecking=no docker-compose.prod.yml ubuntu@$PUBLIC_IP:/opt/booky-app/
      scp -i ~/.ssh/booky_deploy_key -o StrictHostKeyChecking=no "$JAR_FILE" ubuntu@$PUBLIC_IP:/opt/booky-app/target/
      
      echo "🐳 Desplegando aplicación..."
      ssh -i ~/.ssh/booky_deploy_key -o StrictHostKeyChecking=no ubuntu@$PUBLIC_IP "
        cd /opt/booky-app
        docker-compose -f docker-compose.prod.yml down 2>/dev/null || true
        docker-compose -f docker-compose.prod.yml up -d
      "
      
      echo "⏳ Esperando que la aplicación esté lista..."
      sleep 30
      
      echo "✅ Deployment completado!"
      break
    fi
  fi
done

if [[ "$SSH_SUCCESS" == "false" ]]; then
  echo ""
  echo "❌ No se pudo establecer conexión SSH automáticamente"
  echo ""
  echo "🔧 DEPLOYMENT MANUAL REQUERIDO:"
  echo "1. Conéctate a la instancia EC2 usando la consola AWS o key existente"
  echo "2. Ejecuta estos comandos:"
  echo ""
  echo "   # Agregar nueva SSH key"
  echo "   echo '$PUBLIC_KEY' >> ~/.ssh/authorized_keys"
  echo ""
  echo "3. Luego ejecuta:"
  echo "   chmod +x deploy-commands.sh"
  echo "   ./deploy-commands.sh"
  echo ""
  echo "4. Después ejecuta este script nuevamente:"
  echo "   ./scripts/simple-deploy.sh"
  echo ""
  echo "📁 Archivos preparados:"
  echo "   - ${TEMP_KEY_NAME}.pem (nueva key privada)"
  echo "   - docker-compose.prod.yml (configuración de producción)"
  echo "   - deploy-commands.sh (comandos para ejecutar en el servidor)"
  echo ""
else
  echo ""
  echo "🎉 Deployment completado exitosamente!"
  echo "🌐 Aplicación disponible en: http://$PUBLIC_IP"
  echo "📊 Health check: http://$PUBLIC_IP/actuator/health"
  echo "📚 API Docs: http://$PUBLIC_IP/swagger-ui.html"
  echo ""
fi

# Limpiar keys temporales de AWS (opcional)
echo "🧹 Limpiando key temporal de AWS..."
aws ec2 delete-key-pair --region $REGION --key-name $TEMP_KEY_NAME 2>/dev/null || true

echo ""
echo "🔑 Key privada guardada como: ${TEMP_KEY_NAME}.pem"
echo "💡 Guarda esta key para futuros deployments" 