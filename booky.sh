#!/bin/bash

# =====================================================
# 🚀 BOOKY-BE UNIFIED CONTROL SCRIPT
# =====================================================
# Usage: ./booky.sh [start|backend|stop]
#   start   - Full setup: PostgreSQL + Backend + Data
#   backend - Backend only (assumes DB exists)
#   stop    - Stop everything

set -e  # Exit on any error

COMMAND=$1

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
print_header() {
    echo -e "${BLUE}🚀 BOOKY-BE: $1${NC}"
    echo "========================================"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Check if Docker is running
check_docker() {
    if ! docker info >/dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker first."
        exit 1
    fi
}

# Function: Full Start (PostgreSQL + Backend + Data)
full_start() {
    print_header "STARTING COMPLETE APPLICATION"
    
    echo "📋 This will:"
    echo "   - Clean any existing containers"
    echo "   - Start PostgreSQL with fresh data"
    echo "   - Build and start the backend"
    echo "   - Load sample data"
    echo ""
    
    # 1. CLEANUP
    echo "🧹 Cleaning up existing resources..."
    docker stop booky-backend booky-postgres booky-adminer 2>/dev/null || echo "Containers already stopped"
    docker rm booky-backend booky-postgres booky-adminer 2>/dev/null || echo "Containers already removed"
    docker network rm booky-network 2>/dev/null || echo "Network already removed"
    print_success "Cleanup complete"
    
    # 2. CREATE NETWORK
    echo "📡 Creating network..."
    docker network create booky-network
    print_success "Network created"
    
    # 3. START POSTGRESQL
    echo "🗄️  Starting PostgreSQL..."
    docker run -d \
      --name booky-postgres \
      --network booky-network \
      -e POSTGRES_DB=booky \
      -e POSTGRES_USER=postgres \
      -e POSTGRES_PASSWORD=admin \
      -p 5433:5432 \
      -v postgres_data:/var/lib/postgresql/data \
      postgres:15-alpine
    
    echo "⏳ Waiting for PostgreSQL to be ready..."
    sleep 10
    
    while ! docker exec booky-postgres pg_isready -U postgres -d booky >/dev/null 2>&1; do
        echo "   Waiting for database..."
        sleep 2
    done
    print_success "PostgreSQL ready"
    
    # 4. INITIALIZE DATABASE
    echo "🗄️  Initializing database schema..."
    docker cp scripts/database_schema_updated.sql booky-postgres:/tmp/
    docker exec booky-postgres psql -U postgres -d booky -f /tmp/database_schema_updated.sql
    
    # Load sample data
    if [ -f "scripts/alta_usuarios.sql" ]; then
        echo "📊 Loading user data..."
        docker cp scripts/alta_usuarios.sql booky-postgres:/tmp/
        docker exec booky-postgres psql -U postgres -d booky -f /tmp/alta_usuarios.sql
    fi
    
    if [ -f "scripts/alta_comunidades.sql" ]; then
        echo "📊 Loading community data..."
        docker cp scripts/alta_comunidades.sql booky-postgres:/tmp/
        docker exec booky-postgres psql -U postgres -d booky -f /tmp/alta_comunidades.sql
    fi
    
    if [ -f "scripts/alta_clubes_lectura.sql" ]; then
        echo "📊 Loading reading clubs data..."
        docker cp scripts/alta_clubes_lectura.sql booky-postgres:/tmp/
        docker exec booky-postgres psql -U postgres -d booky -f /tmp/alta_clubes_lectura.sql
    fi
    
    print_success "Database initialized with sample data"
    
    # 5. BUILD AND START BACKEND
    start_backend_only
    
    # 6. START ADMINER
    echo "🗄️  Starting Adminer..."
    docker run -d \
      --name booky-adminer \
      --network booky-network \
      -p 8081:8080 \
      adminer:4.8.1
    print_success "Adminer started"
    
    # 7. FINAL STATUS
    show_status
    print_success "Application started successfully!"
}

# Function: Start Backend Only
start_backend_only() {
    if [ "$COMMAND" = "backend" ]; then
        print_header "STARTING BACKEND ONLY"
        
        # Check if PostgreSQL is running
        if ! docker ps | grep -q booky-postgres; then
            print_error "PostgreSQL is not running. Use './booky.sh start' for full setup."
            exit 1
        fi
        
        # Check if network exists
        if ! docker network ls | grep -q booky-network; then
            print_warning "Creating booky-network..."
            docker network create booky-network
        fi
    fi
    
    echo "🔨 Building backend application..."
    docker build --no-cache -t booky-backend .
    print_success "Backend built"
    
    echo "🚀 Starting backend application..."
    # Remove existing backend if running
    docker stop booky-backend 2>/dev/null || echo "Backend not running"
    docker rm booky-backend 2>/dev/null || echo "Backend container not found"
    
    docker run -d \
      --name booky-backend \
      --network booky-network \
      -p 8080:8080 \
      -e DATABASE_URL=jdbc:postgresql://booky-postgres:5432/booky \
      -e DATABASE_USERNAME=postgres \
      -e DATABASE_PASSWORD=admin \
      -e DATABASE_NAME=booky \
      -e SPRING_PROFILES_ACTIVE=local \
      booky-backend
    
    echo "⏳ Waiting for backend to start..."
    sleep 25
    
    # Verify backend is responding
    attempt=0
    max_attempts=10
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "http://localhost:8080/books/search?q=test" >/dev/null 2>&1; then
            print_success "Backend is responding"
            break
        fi
        attempt=$((attempt + 1))
        echo "   Waiting for application... ($attempt/$max_attempts)"
        sleep 5
    done
    
    if [ $attempt -eq $max_attempts ]; then
        print_error "Backend failed to start properly"
        echo "📜 Backend logs:"
        docker logs booky-backend --tail 20
        exit 1
    fi
    
    if [ "$COMMAND" = "backend" ]; then
        show_status
        print_success "Backend started successfully!"
    fi
}

# Function: Stop Everything
stop_all() {
    print_header "STOPPING ALL SERVICES"
    
    echo "📦 Stopping containers..."
    
    # Stop backend
    if docker ps | grep -q booky-backend; then
        echo "   🔴 Stopping backend..."
        docker stop booky-backend
        docker rm booky-backend
    else
        echo "   ⚪ Backend not running"
    fi
    
    # Stop adminer
    if docker ps | grep -q booky-adminer; then
        echo "   🔴 Stopping adminer..."
        docker stop booky-adminer
        docker rm booky-adminer
    else
        echo "   ⚪ Adminer not running"
    fi
    
    # Stop postgres
    if docker ps | grep -q booky-postgres; then
        echo "   🔴 Stopping PostgreSQL..."
        docker stop booky-postgres
        docker rm booky-postgres
    else
        echo "   ⚪ PostgreSQL not running"
    fi
    
    # Remove network
    if docker network ls | grep -q booky-network; then
        echo "   🗑️  Removing network..."
        docker network rm booky-network
    fi
    
    print_success "All services stopped"
    
    # Show final status
    echo ""
    echo "📊 Current status:"
    RUNNING_CONTAINERS=$(docker ps | grep booky | wc -l | tr -d ' ')
    if [ "$RUNNING_CONTAINERS" -eq 0 ]; then
        print_success "All Booky services stopped"
    else
        print_warning "Still running: $RUNNING_CONTAINERS containers"
        docker ps | grep booky
    fi
    
    echo ""
    echo "🔄 To start again:"
    echo "   ./booky.sh start    - Full setup"
    echo "   ./booky.sh backend  - Backend only"
}

# Function: Show Status
show_status() {
    echo ""
    echo "📊 Current Status:"
    docker ps | grep booky || echo "No Booky containers running"
    echo ""
    echo "🌐 Available Services:"
    echo "   📱 API Backend:    http://localhost:8080"
    echo "   📚 Swagger UI:     http://localhost:8080/swagger-ui/index.html"
    if docker ps | grep -q booky-adminer; then
        echo "   🗄️  Adminer:       http://localhost:8081"
    fi
    if docker ps | grep -q booky-postgres; then
        echo "   📊 PostgreSQL:     localhost:5433 (postgres/admin)"
    fi
    echo ""
    echo "🧪 Test endpoints:"
    echo "   curl \"http://localhost:8080/books/search?q=hobbit\""
    echo "   curl \"http://localhost:8080/users\""
    echo ""
}

# Main script logic
case $COMMAND in
    "start")
        check_docker
        full_start
        ;;
    "backend")
        check_docker
        start_backend_only
        ;;
    "stop")
        stop_all
        ;;
    *)
        echo ""
        print_header "BOOKY-BE CONTROL SCRIPT"
        echo ""
        echo "Usage: $0 [command]"
        echo ""
        echo "Commands:"
        echo "  start    🚀 Full setup: PostgreSQL + Backend + Sample Data"
        echo "  backend  ⚡ Backend only (assumes PostgreSQL exists)"
        echo "  stop     🛑 Stop everything (backend + database)"
        echo ""
        echo "Examples:"
        echo "  $0 start     # First time setup"
        echo "  $0 backend   # Quick restart after code changes"
        echo "  $0 stop      # Clean shutdown"
        echo ""
        exit 1
        ;;
esac 