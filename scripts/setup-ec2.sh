#!/bin/bash

# Script para configurar EC2 instance con Docker y Docker Compose
# Autor: Booky Backend Setup
# Este script crea una instancia EC2 si no existe y la configura con Docker

set -e

# Configuración por defecto
INSTANCE_TYPE=${EC2_INSTANCE_TYPE:-t3.medium}
REGION=${AWS_REGION:-us-east-1}
KEY_NAME=${EC2_KEY_NAME:-booky-key}
SECURITY_GROUP=${EC2_SECURITY_GROUP:-booky-sg}
INSTANCE_NAME=${EC2_INSTANCE_NAME:-booky-server}

echo "🚀 Configurando EC2 instance para Booky Backend..."

# Verificar si la instancia ya existe
INSTANCE_ID=$(aws ec2 describe-instances \
  --region $REGION \
  --filters "Name=tag:Name,Values=$INSTANCE_NAME" "Name=instance-state-name,Values=running" \
  --query 'Reservations[0].Instances[0].InstanceId' \
  --output text 2>/dev/null || echo "None")

if [[ "$INSTANCE_ID" == "None" || "$INSTANCE_ID" == "" ]]; then
  echo "📝 Creando nueva instancia EC2..."
  
  # Crear Key Pair si no existe
  if ! aws ec2 describe-key-pairs --region $REGION --key-names $KEY_NAME &> /dev/null; then
    echo "🔑 Creando Key Pair..."
    aws ec2 create-key-pair --region $REGION --key-name $KEY_NAME --query 'KeyMaterial' --output text > ${KEY_NAME}.pem
    chmod 400 ${KEY_NAME}.pem
    echo "⚠️  IMPORTANTE: Guarda el archivo ${KEY_NAME}.pem en un lugar seguro"
  fi
  
  # Crear Security Group si no existe
  if ! aws ec2 describe-security-groups --region $REGION --group-names $SECURITY_GROUP &> /dev/null; then
    echo "🔒 Creando Security Group..."
    SG_ID=$(aws ec2 create-security-group \
      --region $REGION \
      --group-name $SECURITY_GROUP \
      --description "Security group for Booky Backend" \
      --query 'GroupId' \
      --output text)
    
    # Permitir SSH (puerto 22)
    aws ec2 authorize-security-group-ingress \
      --region $REGION \
      --group-id $SG_ID \
      --protocol tcp \
      --port 22 \
      --cidr 0.0.0.0/0
    
    # Permitir HTTP (puerto 80)
    aws ec2 authorize-security-group-ingress \
      --region $REGION \
      --group-id $SG_ID \
      --protocol tcp \
      --port 80 \
      --cidr 0.0.0.0/0
    
    # Permitir HTTPS (puerto 443)
    aws ec2 authorize-security-group-ingress \
      --region $REGION \
      --group-id $SG_ID \
      --protocol tcp \
      --port 443 \
      --cidr 0.0.0.0/0
    
    # Permitir aplicación (puerto 8080)
    aws ec2 authorize-security-group-ingress \
      --region $REGION \
      --group-id $SG_ID \
      --protocol tcp \
      --port 8080 \
      --cidr 0.0.0.0/0
    
    echo "✅ Security Group creado: $SG_ID"
  fi
  
  # Obtener la AMI de Ubuntu más reciente
  AMI_ID=$(aws ec2 describe-images \
    --region $REGION \
    --owners 099720109477 \
    --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
    --query 'Images | sort_by(@, &CreationDate) | [-1].ImageId' \
    --output text)
  
  echo "🖥️  Usando AMI: $AMI_ID"
  
  # Script de inicialización
  USER_DATA=$(cat << 'EOF'
#!/bin/bash
apt-get update
apt-get install -y docker.io docker-compose git awscli

# Iniciar Docker
systemctl start docker
systemctl enable docker

# Añadir usuario ubuntu al grupo docker
usermod -aG docker ubuntu

# Instalar Docker Compose v2
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

# Crear directorios para la aplicación
mkdir -p /opt/booky-app
chown ubuntu:ubuntu /opt/booky-app

# Instalar nginx para reverse proxy
apt-get install -y nginx

# Configurar nginx como reverse proxy
cat > /etc/nginx/sites-available/booky << 'NGINXCONF'
server {
    listen 80;
    server_name _;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
NGINXCONF

# Habilitar el sitio
ln -s /etc/nginx/sites-available/booky /etc/nginx/sites-enabled/
rm -f /etc/nginx/sites-enabled/default
systemctl restart nginx
systemctl enable nginx

echo "✅ EC2 instance configurada correctamente" > /tmp/setup-complete.log
EOF
)
  
  # Crear la instancia
  echo "🏗️  Creando instancia EC2..."
  INSTANCE_ID=$(aws ec2 run-instances \
    --region $REGION \
    --image-id $AMI_ID \
    --instance-type $INSTANCE_TYPE \
    --key-name $KEY_NAME \
    --security-groups $SECURITY_GROUP \
    --user-data "$USER_DATA" \
    --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME}]" \
    --query 'Instances[0].InstanceId' \
    --output text)
  
  echo "⏳ Esperando que la instancia esté lista..."
  aws ec2 wait instance-running --region $REGION --instance-ids $INSTANCE_ID
  
  echo "✅ Instancia creada: $INSTANCE_ID"
else
  echo "✅ Instancia ya existe: $INSTANCE_ID"
fi

# Obtener la IP pública
PUBLIC_IP=$(aws ec2 describe-instances \
  --region $REGION \
  --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo "🌐 IP Pública: $PUBLIC_IP"
echo "📋 Configuración completada!"
echo ""
echo "Para conectarte por SSH:"
echo "ssh -i ${KEY_NAME}.pem ubuntu@$PUBLIC_IP"
echo ""
echo "⚠️  IMPORTANTE: Configura las siguientes variables en GitHub Secrets:"
echo "- EC2_HOST: $PUBLIC_IP"
echo "- EC2_USER: ubuntu"
echo "- EC2_PRIVATE_KEY: (contenido del archivo ${KEY_NAME}.pem)"
echo "- AWS_ACCESS_KEY_ID: (tu AWS Access Key ID)"
echo "- AWS_SECRET_ACCESS_KEY: (tu AWS Secret Access Key)"
echo "- AWS_REGION: $REGION"

# Exportar variables para el siguiente paso
echo "EC2_HOST=$PUBLIC_IP" >> $GITHUB_ENV
echo "EC2_USER=ubuntu" >> $GITHUB_ENV 