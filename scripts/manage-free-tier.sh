#!/bin/bash

# Configuration
INSTANCE_NAME="booky-app"
DB_INSTANCE_ID="booky-db"
KEY_NAME="booky-keypair"
SECURITY_GROUP="booky-sg"
REGION="us-east-1"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to get instance info
get_instance_info() {
    aws ec2 describe-instances \
        --filters "Name=tag:Name,Values=$INSTANCE_NAME" "Name=instance-state-name,Values=running,pending,stopping,stopped" \
        --query 'Reservations[0].Instances[0]' \
        --output json 2>/dev/null
}

# Function to get RDS info
get_rds_info() {
    aws rds describe-db-instances \
        --db-instance-identifier "$DB_INSTANCE_ID" \
        --query 'DBInstances[0]' \
        --output json 2>/dev/null
}

# Function to check status
check_status() {
    echo -e "${BLUE}🔍 Checking Booky Free Tier Status${NC}"
    echo "=================================="
    
    # EC2 Instance Status
    echo -e "${YELLOW}EC2 Instance:${NC}"
    INSTANCE_INFO=$(get_instance_info)
    if [ "$INSTANCE_INFO" != "null" ] && [ -n "$INSTANCE_INFO" ]; then
        INSTANCE_ID=$(echo "$INSTANCE_INFO" | jq -r '.InstanceId')
        INSTANCE_STATE=$(echo "$INSTANCE_INFO" | jq -r '.State.Name')
        PUBLIC_IP=$(echo "$INSTANCE_INFO" | jq -r '.PublicIpAddress')
        INSTANCE_TYPE=$(echo "$INSTANCE_INFO" | jq -r '.InstanceType')
        
        echo "  ID: $INSTANCE_ID"
        echo "  State: $INSTANCE_STATE"
        echo "  Type: $INSTANCE_TYPE"
        echo "  Public IP: $PUBLIC_IP"
        
        if [ "$INSTANCE_STATE" == "running" ]; then
            echo -e "  Status: ${GREEN}✅ Running${NC}"
        else
            echo -e "  Status: ${RED}❌ $INSTANCE_STATE${NC}"
        fi
    else
        echo -e "  Status: ${RED}❌ Not found${NC}"
    fi
    
    echo ""
    
    # RDS Status
    echo -e "${YELLOW}RDS Database:${NC}"
    RDS_INFO=$(get_rds_info)
    if [ "$RDS_INFO" != "null" ] && [ -n "$RDS_INFO" ]; then
        DB_STATUS=$(echo "$RDS_INFO" | jq -r '.DBInstanceStatus')
        DB_ENGINE=$(echo "$RDS_INFO" | jq -r '.Engine')
        DB_VERSION=$(echo "$RDS_INFO" | jq -r '.EngineVersion')
        DB_CLASS=$(echo "$RDS_INFO" | jq -r '.DBInstanceClass')
        DB_STORAGE=$(echo "$RDS_INFO" | jq -r '.AllocatedStorage')
        DB_ENDPOINT=$(echo "$RDS_INFO" | jq -r '.Endpoint.Address')
        
        echo "  Engine: $DB_ENGINE $DB_VERSION"
        echo "  Class: $DB_CLASS"
        echo "  Storage: ${DB_STORAGE}GB"
        echo "  Endpoint: $DB_ENDPOINT"
        
        if [ "$DB_STATUS" == "available" ]; then
            echo -e "  Status: ${GREEN}✅ Available${NC}"
        else
            echo -e "  Status: ${YELLOW}⏳ $DB_STATUS${NC}"
        fi
    else
        echo -e "  Status: ${RED}❌ Not found${NC}"
    fi
    
    echo ""
    
    # Application Status
    if [ "$INSTANCE_STATE" == "running" ] && [ "$PUBLIC_IP" != "null" ]; then
        echo -e "${YELLOW}Application:${NC}"
        echo "  URL: http://$PUBLIC_IP"
        
        # Check if application is responding
        if curl -s --connect-timeout 5 "http://$PUBLIC_IP/health" >/dev/null 2>&1; then
            echo -e "  Status: ${GREEN}✅ Healthy${NC}"
        else
            echo -e "  Status: ${RED}❌ Not responding${NC}"
        fi
    fi
}

# Function to check costs
check_costs() {
    echo -e "${BLUE}💰 AWS Free Tier Usage & Costs${NC}"
    echo "==============================="
    
    # Get current month
    CURRENT_MONTH=$(date +%Y-%m)
    START_DATE="${CURRENT_MONTH}-01"
    END_DATE=$(date +%Y-%m-%d)
    
    # EC2 Usage
    echo -e "${YELLOW}EC2 Usage:${NC}"
    aws ce get-dimension-values \
        --time-period Start=$START_DATE,End=$END_DATE \
        --dimension EC2_INSTANCE_TYPE \
        --context COST_AND_USAGE \
        --query 'DimensionValues[?Value==`t2.micro`]' \
        --output table 2>/dev/null || echo "  Unable to fetch EC2 usage data"
    
    # RDS Usage
    echo -e "${YELLOW}RDS Usage:${NC}"
    aws ce get-dimension-values \
        --time-period Start=$START_DATE,End=$END_DATE \
        --dimension DATABASE_ENGINE \
        --context COST_AND_USAGE \
        --query 'DimensionValues[?Value==`PostgreSQL`]' \
        --output table 2>/dev/null || echo "  Unable to fetch RDS usage data"
    
    # Total costs
    echo -e "${YELLOW}Total Costs (Current Month):${NC}"
    aws ce get-cost-and-usage \
        --time-period Start=$START_DATE,End=$END_DATE \
        --granularity MONTHLY \
        --metrics BlendedCost \
        --query 'ResultsByTime[0].Total.BlendedCost' \
        --output table 2>/dev/null || echo "  Unable to fetch cost data"
    
    echo ""
    echo -e "${GREEN}💡 Free Tier Limits:${NC}"
    echo "  • EC2 t2.micro: 750 hours/month"
    echo "  • RDS db.t3.micro: 750 hours/month"
    echo "  • RDS Storage: 20GB"
    echo "  • Data Transfer: 15GB out/month"
}

# Function to view logs
view_logs() {
    echo -e "${BLUE}📋 Application Logs${NC}"
    echo "==================="
    
    INSTANCE_INFO=$(get_instance_info)
    if [ "$INSTANCE_INFO" != "null" ] && [ -n "$INSTANCE_INFO" ]; then
        PUBLIC_IP=$(echo "$INSTANCE_INFO" | jq -r '.PublicIpAddress')
        INSTANCE_STATE=$(echo "$INSTANCE_INFO" | jq -r '.State.Name')
        
        if [ "$INSTANCE_STATE" == "running" ]; then
            echo "Connecting to $PUBLIC_IP..."
            ssh -i ~/.ssh/${KEY_NAME}.pem -o StrictHostKeyChecking=no ec2-user@${PUBLIC_IP} << 'ENDSSH'
echo "=== Application Logs ==="
cd /opt/booky
docker-compose -f docker-compose.free-tier.yml logs --tail=50 booky-app

echo -e "\n=== Nginx Logs ==="
docker-compose -f docker-compose.free-tier.yml logs --tail=20 nginx

echo -e "\n=== System Resources ==="
free -h
df -h
ENDSSH
        else
            echo -e "${RED}❌ Instance is not running${NC}"
        fi
    else
        echo -e "${RED}❌ Instance not found${NC}"
    fi
}

# Function to restart application
restart_app() {
    echo -e "${BLUE}🔄 Restarting Application${NC}"
    echo "========================="
    
    INSTANCE_INFO=$(get_instance_info)
    if [ "$INSTANCE_INFO" != "null" ] && [ -n "$INSTANCE_INFO" ]; then
        PUBLIC_IP=$(echo "$INSTANCE_INFO" | jq -r '.PublicIpAddress')
        INSTANCE_STATE=$(echo "$INSTANCE_INFO" | jq -r '.State.Name')
        
        if [ "$INSTANCE_STATE" == "running" ]; then
            echo "Restarting application on $PUBLIC_IP..."
            ssh -i ~/.ssh/${KEY_NAME}.pem -o StrictHostKeyChecking=no ec2-user@${PUBLIC_IP} << 'ENDSSH'
cd /opt/booky
docker-compose -f docker-compose.free-tier.yml restart
echo "Application restarted successfully!"
ENDSSH
        else
            echo -e "${RED}❌ Instance is not running${NC}"
        fi
    else
        echo -e "${RED}❌ Instance not found${NC}"
    fi
}

# Function to stop resources
stop_resources() {
    echo -e "${BLUE}⏹️  Stopping Resources${NC}"
    echo "===================="
    
    read -p "Are you sure you want to stop all resources? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cancelled"
        return
    fi
    
    # Stop EC2 instance
    INSTANCE_INFO=$(get_instance_info)
    if [ "$INSTANCE_INFO" != "null" ] && [ -n "$INSTANCE_INFO" ]; then
        INSTANCE_ID=$(echo "$INSTANCE_INFO" | jq -r '.InstanceId')
        echo "Stopping EC2 instance $INSTANCE_ID..."
        aws ec2 stop-instances --instance-ids "$INSTANCE_ID"
        aws ec2 wait instance-stopped --instance-ids "$INSTANCE_ID"
        echo "EC2 instance stopped"
    fi
    
    # Stop RDS instance
    RDS_INFO=$(get_rds_info)
    if [ "$RDS_INFO" != "null" ] && [ -n "$RDS_INFO" ]; then
        echo "Stopping RDS instance $DB_INSTANCE_ID..."
        aws rds stop-db-instance --db-instance-identifier "$DB_INSTANCE_ID"
        echo "RDS instance stop initiated"
    fi
    
    echo -e "${GREEN}✅ Resources stopped${NC}"
}

# Function to start resources
start_resources() {
    echo -e "${BLUE}▶️  Starting Resources${NC}"
    echo "==================="
    
    # Start EC2 instance
    INSTANCE_INFO=$(get_instance_info)
    if [ "$INSTANCE_INFO" != "null" ] && [ -n "$INSTANCE_INFO" ]; then
        INSTANCE_ID=$(echo "$INSTANCE_INFO" | jq -r '.InstanceId')
        INSTANCE_STATE=$(echo "$INSTANCE_INFO" | jq -r '.State.Name')
        
        if [ "$INSTANCE_STATE" == "stopped" ]; then
            echo "Starting EC2 instance $INSTANCE_ID..."
            aws ec2 start-instances --instance-ids "$INSTANCE_ID"
            aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
            echo "EC2 instance started"
        else
            echo "EC2 instance is already $INSTANCE_STATE"
        fi
    fi
    
    # Start RDS instance
    RDS_INFO=$(get_rds_info)
    if [ "$RDS_INFO" != "null" ] && [ -n "$RDS_INFO" ]; then
        DB_STATUS=$(echo "$RDS_INFO" | jq -r '.DBInstanceStatus')
        
        if [ "$DB_STATUS" == "stopped" ]; then
            echo "Starting RDS instance $DB_INSTANCE_ID..."
            aws rds start-db-instance --db-instance-identifier "$DB_INSTANCE_ID"
            echo "RDS instance start initiated"
        else
            echo "RDS instance is already $DB_STATUS"
        fi
    fi
    
    echo -e "${GREEN}✅ Resources started${NC}"
}

# Function to monitor resources
monitor_resources() {
    echo -e "${BLUE}📊 Resource Monitoring${NC}"
    echo "====================="
    
    while true; do
        clear
        echo -e "${BLUE}📊 Resource Monitoring (Press Ctrl+C to exit)${NC}"
        echo "=============================================="
        date
        echo ""
        
        # EC2 Status
        INSTANCE_INFO=$(get_instance_info)
        if [ "$INSTANCE_INFO" != "null" ] && [ -n "$INSTANCE_INFO" ]; then
            INSTANCE_STATE=$(echo "$INSTANCE_INFO" | jq -r '.State.Name')
            PUBLIC_IP=$(echo "$INSTANCE_INFO" | jq -r '.PublicIpAddress')
            
            echo -e "${YELLOW}EC2 Instance:${NC} $INSTANCE_STATE"
            
            if [ "$INSTANCE_STATE" == "running" ]; then
                # Get CloudWatch metrics
                aws cloudwatch get-metric-statistics \
                    --namespace AWS/EC2 \
                    --metric-name CPUUtilization \
                    --dimensions Name=InstanceId,Value=$(echo "$INSTANCE_INFO" | jq -r '.InstanceId') \
                    --start-time $(date -u -d '5 minutes ago' +%Y-%m-%dT%H:%M:%S) \
                    --end-time $(date -u +%Y-%m-%dT%H:%M:%S) \
                    --period 300 \
                    --statistics Average \
                    --query 'Datapoints[0].Average' \
                    --output text 2>/dev/null | head -1 | xargs -I {} echo "CPU Usage: {}%"
            fi
        fi
        
        # RDS Status
        RDS_INFO=$(get_rds_info)
        if [ "$RDS_INFO" != "null" ] && [ -n "$RDS_INFO" ]; then
            DB_STATUS=$(echo "$RDS_INFO" | jq -r '.DBInstanceStatus')
            echo -e "${YELLOW}RDS Database:${NC} $DB_STATUS"
        fi
        
        echo ""
        echo "Next refresh in 30 seconds..."
        sleep 30
    done
}

# Function to SSH into instance
ssh_instance() {
    echo -e "${BLUE}🔑 SSH Access${NC}"
    echo "============="
    
    INSTANCE_INFO=$(get_instance_info)
    if [ "$INSTANCE_INFO" != "null" ] && [ -n "$INSTANCE_INFO" ]; then
        PUBLIC_IP=$(echo "$INSTANCE_INFO" | jq -r '.PublicIpAddress')
        INSTANCE_STATE=$(echo "$INSTANCE_INFO" | jq -r '.State.Name')
        
        if [ "$INSTANCE_STATE" == "running" ]; then
            echo "Connecting to $PUBLIC_IP..."
            ssh -i ~/.ssh/${KEY_NAME}.pem -o StrictHostKeyChecking=no ec2-user@${PUBLIC_IP}
        else
            echo -e "${RED}❌ Instance is not running${NC}"
        fi
    else
        echo -e "${RED}❌ Instance not found${NC}"
    fi
}

# Function to reinitialize database
init_database() {
    echo -e "${BLUE}🗄️  Reinitializing Database${NC}"
    echo "=========================="
    
    read -p "Are you sure you want to reinitialize the database? This will delete all data! (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cancelled"
        return
    fi
    
    INSTANCE_INFO=$(get_instance_info)
    RDS_INFO=$(get_rds_info)
    
    if [ "$INSTANCE_INFO" != "null" ] && [ "$RDS_INFO" != "null" ]; then
        PUBLIC_IP=$(echo "$INSTANCE_INFO" | jq -r '.PublicIpAddress')
        INSTANCE_STATE=$(echo "$INSTANCE_INFO" | jq -r '.State.Name')
        DB_STATUS=$(echo "$RDS_INFO" | jq -r '.DBInstanceStatus')
        DB_ENDPOINT=$(echo "$RDS_INFO" | jq -r '.Endpoint.Address')
        
        if [ "$INSTANCE_STATE" == "running" ] && [ "$DB_STATUS" == "available" ]; then
            echo "Reinitializing database..."
            ssh -i ~/.ssh/${KEY_NAME}.pem -o StrictHostKeyChecking=no ec2-user@${PUBLIC_IP} << ENDSSH
cd /opt/booky
source env.free-tier
PGPASSWORD="$DB_PASSWORD" psql -h "$DB_ENDPOINT" -U "$DB_USERNAME" -d "$DB_NAME" -f scripts/init-database-rds.sql
echo "Database reinitialized successfully!"
ENDSSH
        else
            echo -e "${RED}❌ Resources are not available${NC}"
        fi
    else
        echo -e "${RED}❌ Resources not found${NC}"
    fi
}

# Function to cleanup resources
cleanup_resources() {
    echo -e "${BLUE}🧹 Cleaning up Resources${NC}"
    echo "========================"
    
    read -p "Are you sure you want to delete all resources? This cannot be undone! (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Cancelled"
        return
    fi
    
    echo "Deleting resources..."
    
    # Terminate EC2 instance
    INSTANCE_INFO=$(get_instance_info)
    if [ "$INSTANCE_INFO" != "null" ] && [ -n "$INSTANCE_INFO" ]; then
        INSTANCE_ID=$(echo "$INSTANCE_INFO" | jq -r '.InstanceId')
        echo "Terminating EC2 instance $INSTANCE_ID..."
        aws ec2 terminate-instances --instance-ids "$INSTANCE_ID"
        aws ec2 wait instance-terminated --instance-ids "$INSTANCE_ID"
        echo "EC2 instance terminated"
    fi
    
    # Delete RDS instance
    RDS_INFO=$(get_rds_info)
    if [ "$RDS_INFO" != "null" ] && [ -n "$RDS_INFO" ]; then
        echo "Deleting RDS instance $DB_INSTANCE_ID..."
        aws rds delete-db-instance \
            --db-instance-identifier "$DB_INSTANCE_ID" \
            --skip-final-snapshot
        echo "RDS instance deletion initiated"
    fi
    
    # Delete security group
    echo "Deleting security group..."
    aws ec2 delete-security-group --group-name "$SECURITY_GROUP" 2>/dev/null || true
    
    # Delete key pair
    echo "Deleting key pair..."
    aws ec2 delete-key-pair --key-name "$KEY_NAME" 2>/dev/null || true
    rm -f ~/.ssh/${KEY_NAME}.pem
    
    # Delete RDS subnet group
    echo "Deleting RDS subnet group..."
    aws rds delete-db-subnet-group --db-subnet-group-name "booky-subnet-group" 2>/dev/null || true
    
    echo -e "${GREEN}✅ Cleanup completed${NC}"
}

# Function to completely destroy deployment
destroy_deployment() {
    echo -e "${RED}💥 DESTROYING Deployment${NC}"
    echo "========================"
    
    echo -e "${RED}WARNING: This will permanently delete ALL resources and data!${NC}"
    echo "This action cannot be undone."
    echo ""
    read -p "Type 'DELETE' to confirm: " -r
    if [[ $REPLY != "DELETE" ]]; then
        echo "Cancelled"
        return
    fi
    
    cleanup_resources
    
    echo -e "${GREEN}✅ Deployment destroyed${NC}"
}

# Function to show help
show_help() {
    echo -e "${BLUE}🛠️  Booky Free Tier Management${NC}"
    echo "=============================="
    echo ""
    echo "Usage: $0 [command]"
    echo ""
    echo "Commands:"
    echo "  status      Check deployment status"
    echo "  costs       Show AWS costs and usage"
    echo "  logs        View application logs"
    echo "  restart     Restart application"
    echo "  stop        Stop all resources"
    echo "  start       Start all resources"
    echo "  monitor     Monitor resources in real-time"
    echo "  ssh         SSH into EC2 instance"
    echo "  init-db     Reinitialize database"
    echo "  cleanup     Delete all resources"
    echo "  destroy     Permanently destroy deployment"
    echo "  help        Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 status"
    echo "  $0 logs"
    echo "  $0 ssh"
}

# Main script
case "$1" in
    "status")
        check_status
        ;;
    "costs")
        check_costs
        ;;
    "logs")
        view_logs
        ;;
    "restart")
        restart_app
        ;;
    "stop")
        stop_resources
        ;;
    "start")
        start_resources
        ;;
    "monitor")
        monitor_resources
        ;;
    "ssh")
        ssh_instance
        ;;
    "init-db")
        init_database
        ;;
    "cleanup")
        cleanup_resources
        ;;
    "destroy")
        destroy_deployment
        ;;
    "help"|"")
        show_help
        ;;
    *)
        echo -e "${RED}❌ Unknown command: $1${NC}"
        echo ""
        show_help
        exit 1
        ;;
esac 