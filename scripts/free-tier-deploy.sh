#!/bin/bash

set -e

# Configuration
INSTANCE_TYPE="t2.micro"
KEY_NAME="booky-keypair"
SECURITY_GROUP="booky-sg"
DB_INSTANCE_ID="booky-db"
DB_ENGINE="postgres"
DB_VERSION="13.13"
DB_CLASS="db.t3.micro"
DB_STORAGE="20"
DB_USERNAME="booky"
DB_PASSWORD="BookyPassword123!"
DB_NAME="booky"
REGION="us-east-1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}🚀 Starting Booky Free Tier Deployment${NC}"
echo "=============================================="

# Check if this is AWS Sandbox
if aws organizations describe-organization &>/dev/null; then
    echo -e "${YELLOW}⚠️  AWS Sandbox detected. Some features may be limited.${NC}"
    SANDBOX=true
else
    echo -e "${GREEN}✅ Real AWS account detected. Full Free Tier available.${NC}"
    SANDBOX=false
fi

# Function to check if resource exists
check_resource() {
    local resource_type=$1
    local resource_name=$2
    
    case $resource_type in
        "keypair")
            aws ec2 describe-key-pairs --key-names "$resource_name" &>/dev/null
            ;;
        "security-group")
            aws ec2 describe-security-groups --group-names "$resource_name" &>/dev/null
            ;;
        "rds")
            aws rds describe-db-instances --db-instance-identifier "$resource_name" &>/dev/null
            ;;
        "instance")
            aws ec2 describe-instances --filters "Name=tag:Name,Values=$resource_name" "Name=instance-state-name,Values=running,pending,stopping,stopped" &>/dev/null
            ;;
    esac
}

# Create or import key pair
echo -e "${YELLOW}🔑 Setting up key pair...${NC}"
if check_resource "keypair" "$KEY_NAME"; then
    echo "Key pair $KEY_NAME already exists"
else
    aws ec2 create-key-pair --key-name "$KEY_NAME" --query 'KeyMaterial' --output text > ~/.ssh/${KEY_NAME}.pem
    chmod 600 ~/.ssh/${KEY_NAME}.pem
    echo "Key pair $KEY_NAME created and saved to ~/.ssh/${KEY_NAME}.pem"
fi

# Get default VPC
VPC_ID=$(aws ec2 describe-vpcs --filters "Name=isDefault,Values=true" --query 'Vpcs[0].VpcId' --output text)
echo "Using VPC: $VPC_ID"

# Create security group
echo -e "${YELLOW}🛡️  Setting up security group...${NC}"
if check_resource "security-group" "$SECURITY_GROUP"; then
    SECURITY_GROUP_ID=$(aws ec2 describe-security-groups --group-names "$SECURITY_GROUP" --query 'SecurityGroups[0].GroupId' --output text)
    echo "Security group $SECURITY_GROUP already exists: $SECURITY_GROUP_ID"
else
    SECURITY_GROUP_ID=$(aws ec2 create-security-group \
        --group-name "$SECURITY_GROUP" \
        --description "Security group for Booky application" \
        --vpc-id "$VPC_ID" \
        --query 'GroupId' \
        --output text)
    
    # Add rules
    aws ec2 authorize-security-group-ingress \
        --group-id "$SECURITY_GROUP_ID" \
        --protocol tcp \
        --port 22 \
        --cidr 0.0.0.0/0
    
    aws ec2 authorize-security-group-ingress \
        --group-id "$SECURITY_GROUP_ID" \
        --protocol tcp \
        --port 80 \
        --cidr 0.0.0.0/0
    
    aws ec2 authorize-security-group-ingress \
        --group-id "$SECURITY_GROUP_ID" \
        --protocol tcp \
        --port 443 \
        --cidr 0.0.0.0/0
    
    echo "Security group $SECURITY_GROUP created: $SECURITY_GROUP_ID"
fi

# Create RDS subnet group
echo -e "${YELLOW}🗄️  Setting up RDS subnet group...${NC}"
SUBNET_GROUP_NAME="booky-subnet-group"
if ! aws rds describe-db-subnet-groups --db-subnet-group-name "$SUBNET_GROUP_NAME" &>/dev/null; then
    # Get all subnets in the VPC
    SUBNET_IDS=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=$VPC_ID" --query 'Subnets[].SubnetId' --output text)
    
    aws rds create-db-subnet-group \
        --db-subnet-group-name "$SUBNET_GROUP_NAME" \
        --db-subnet-group-description "Subnet group for Booky RDS" \
        --subnet-ids $SUBNET_IDS
    
    echo "RDS subnet group $SUBNET_GROUP_NAME created"
else
    echo "RDS subnet group $SUBNET_GROUP_NAME already exists"
fi

# Create RDS instance
echo -e "${YELLOW}🗄️  Setting up RDS database...${NC}"
if check_resource "rds" "$DB_INSTANCE_ID"; then
    echo "RDS instance $DB_INSTANCE_ID already exists"
    DB_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_ID" --query 'DBInstances[0].Endpoint.Address' --output text)
else
    echo "Creating RDS instance... This may take 10-15 minutes"
    aws rds create-db-instance \
        --db-instance-identifier "$DB_INSTANCE_ID" \
        --db-instance-class "$DB_CLASS" \
        --engine "$DB_ENGINE" \
        --engine-version "$DB_VERSION" \
        --master-username "$DB_USERNAME" \
        --master-user-password "$DB_PASSWORD" \
        --db-name "$DB_NAME" \
        --allocated-storage "$DB_STORAGE" \
        --vpc-security-group-ids "$SECURITY_GROUP_ID" \
        --db-subnet-group-name "$SUBNET_GROUP_NAME" \
        --backup-retention-period 0 \
        --no-multi-az \
        --storage-type gp2 \
        --no-publicly-accessible \
        --no-auto-minor-version-upgrade
    
    echo "Waiting for RDS instance to be available..."
    aws rds wait db-instance-available --db-instance-identifier "$DB_INSTANCE_ID"
    
    DB_ENDPOINT=$(aws rds describe-db-instances --db-instance-identifier "$DB_INSTANCE_ID" --query 'DBInstances[0].Endpoint.Address' --output text)
    echo "RDS instance created. Endpoint: $DB_ENDPOINT"
fi

# Get latest Amazon Linux 2 AMI
echo -e "${YELLOW}🖥️  Getting latest Amazon Linux 2 AMI...${NC}"
AMI_ID=$(aws ec2 describe-images \
    --owners amazon \
    --filters "Name=name,Values=amzn2-ami-hvm-*-x86_64-gp2" \
    --query 'Images | sort_by(@, &CreationDate) | [-1].ImageId' \
    --output text)

echo "Using AMI: $AMI_ID"

# Create EC2 instance
echo -e "${YELLOW}🖥️  Creating EC2 instance...${NC}"
INSTANCE_NAME="booky-app"
if check_resource "instance" "$INSTANCE_NAME"; then
    echo "Instance $INSTANCE_NAME already exists"
    INSTANCE_ID=$(aws ec2 describe-instances \
        --filters "Name=tag:Name,Values=$INSTANCE_NAME" "Name=instance-state-name,Values=running,pending,stopping,stopped" \
        --query 'Reservations[0].Instances[0].InstanceId' \
        --output text)
else
    # Create user data script
    cat > /tmp/user-data.sh << 'EOF'
#!/bin/bash
yum update -y
yum install -y docker git postgresql

# Configure swap (important for t2.micro)
sudo dd if=/dev/zero of=/swapfile bs=1024 count=1048576
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap defaults 0 0' >> /etc/fstab

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Start Docker
systemctl start docker
systemctl enable docker
usermod -aG docker ec2-user

# Create deployment directory
mkdir -p /opt/booky
chown ec2-user:ec2-user /opt/booky
EOF

    INSTANCE_ID=$(aws ec2 run-instances \
        --image-id "$AMI_ID" \
        --count 1 \
        --instance-type "$INSTANCE_TYPE" \
        --key-name "$KEY_NAME" \
        --security-group-ids "$SECURITY_GROUP_ID" \
        --user-data file:///tmp/user-data.sh \
        --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME}]" \
        --query 'Instances[0].InstanceId' \
        --output text)
    
    echo "Instance $INSTANCE_ID created. Waiting for it to be running..."
    aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
fi

# Get instance public IP
PUBLIC_IP=$(aws ec2 describe-instances \
    --instance-ids "$INSTANCE_ID" \
    --query 'Reservations[0].Instances[0].PublicIpAddress' \
    --output text)

echo "Instance public IP: $PUBLIC_IP"

# Wait for instance to be ready
echo -e "${YELLOW}⏳ Waiting for instance to be ready...${NC}"
sleep 60

# Create environment file
echo -e "${YELLOW}📝 Creating environment configuration...${NC}"
cat > env.free-tier << EOF
# Database Configuration
DB_HOST=$DB_ENDPOINT
DB_PORT=5432
DB_NAME=$DB_NAME
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD

# Application Configuration
SPRING_PROFILES_ACTIVE=prod
SERVER_PORT=8080
JWT_SECRET=your-super-secret-jwt-key-change-this-in-production

# Free Tier Optimizations
JAVA_OPTS=-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200
EOF

# Create optimized docker-compose for Free Tier
cat > docker-compose.free-tier.yml << 'EOF'
version: '3.8'

services:
  booky-app:
    build: .
    container_name: booky-app
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=${DB_HOST}
      - DB_PORT=${DB_PORT}
      - DB_NAME=${DB_NAME}
      - DB_USERNAME=${DB_USERNAME}
      - DB_PASSWORD=${DB_PASSWORD}
      - JAVA_OPTS=${JAVA_OPTS}
    mem_limit: 512m
    mem_reservation: 256m
    cpus: 0.5
    depends_on:
      - nginx
    networks:
      - booky-network

  nginx:
    image: nginx:alpine
    container_name: booky-nginx
    restart: unless-stopped
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
    depends_on:
      - booky-app
    networks:
      - booky-network

networks:
  booky-network:
    driver: bridge
EOF

# Create nginx configuration
cat > nginx.conf << 'EOF'
events {
    worker_connections 1024;
}

http {
    upstream booky-app {
        server booky-app:8080;
    }

    server {
        listen 80;
        server_name _;

        client_max_body_size 10M;

        location / {
            proxy_pass http://booky-app;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        location /health {
            access_log off;
            return 200 "healthy\n";
        }
    }
}
EOF

# Upload files to EC2
echo -e "${YELLOW}📤 Uploading application files...${NC}"
# Package the application if needed
if [ ! -f "target/booky-*.jar" ]; then
    echo "Building application..."
    mvn clean package -DskipTests
fi

# Create deployment package
tar -czf booky-deployment.tar.gz \
    target/booky-*.jar \
    docker-compose.free-tier.yml \
    nginx.conf \
    env.free-tier \
    Dockerfile \
    scripts/init-database-rds.sql \
    scripts/database_schema_updated.sql

# Upload to EC2
scp -i ~/.ssh/${KEY_NAME}.pem -o StrictHostKeyChecking=no \
    booky-deployment.tar.gz \
    ec2-user@${PUBLIC_IP}:/opt/booky/

# Deploy on EC2
echo -e "${YELLOW}🚀 Deploying application...${NC}"
ssh -i ~/.ssh/${KEY_NAME}.pem -o StrictHostKeyChecking=no ec2-user@${PUBLIC_IP} << 'ENDSSH'
cd /opt/booky
tar -xzf booky-deployment.tar.gz
source env.free-tier
docker-compose -f docker-compose.free-tier.yml up -d --build
ENDSSH

# Initialize database
echo -e "${YELLOW}🗄️  Initializing database...${NC}"
sleep 30  # Wait for application to start
ssh -i ~/.ssh/${KEY_NAME}.pem -o StrictHostKeyChecking=no ec2-user@${PUBLIC_IP} << ENDSSH
cd /opt/booky
# Wait for app to be ready and create tables
sleep 60
# Run database initialization
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_ENDPOINT" -U "$DB_USERNAME" -d "$DB_NAME" -f scripts/init-database-rds.sql
ENDSSH

echo -e "${GREEN}✅ Deployment completed successfully!${NC}"
echo "=============================================="
echo -e "${GREEN}🌐 Application URL: http://${PUBLIC_IP}${NC}"
echo -e "${GREEN}🔑 SSH Access: ssh -i ~/.ssh/${KEY_NAME}.pem ec2-user@${PUBLIC_IP}${NC}"
echo -e "${GREEN}💾 Database Endpoint: ${DB_ENDPOINT}${NC}"
echo ""
echo "Test users available:"
echo "- admin@booky.com (password: password123) - Admin role"
echo "- superadmin@booky.com (password: password123) - Super Admin role"
echo "- user1@booky.com through user8@booky.com (password: password123) - Regular users"
echo ""
echo "Management commands:"
echo "- ./scripts/manage-free-tier.sh status    # Check deployment status"
echo "- ./scripts/manage-free-tier.sh costs     # Check AWS costs"
echo "- ./scripts/manage-free-tier.sh logs      # View application logs"
echo "- ./scripts/manage-free-tier.sh ssh       # SSH into instance"
echo "- ./scripts/manage-free-tier.sh cleanup   # Remove deployment"

# Clean up
rm -f booky-deployment.tar.gz env.free-tier docker-compose.free-tier.yml nginx.conf /tmp/user-data.sh 