#!/bin/bash

# 🚀 Booky Backend - Docker Runner Script
# Este script facilita la ejecución de la aplicación con Docker

set -e  # Exit on any error

echo "🚀 Booky Backend - Docker Runner"
echo "================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_success "Docker is running ✓"
}

# Function to start the application
start_app() {
    print_status "Starting Booky Backend application..."
    
    # Build and start containers
    docker-compose up --build -d
    
    print_success "Containers started!"
    print_status "Waiting for services to be ready..."
    
    # Wait for PostgreSQL to be ready
    print_status "Waiting for PostgreSQL..."
    timeout=60
    counter=0
    until docker-compose exec postgres pg_isready -U postgres -d booky > /dev/null 2>&1; do
        sleep 2
        counter=$((counter + 2))
        if [ $counter -ge $timeout ]; then
            print_error "PostgreSQL failed to start within $timeout seconds"
            docker-compose logs postgres
            exit 1
        fi
    done
    print_success "PostgreSQL is ready ✓"
    
    # Wait for the application to be ready
    print_status "Waiting for Spring Boot application..."
    timeout=120
    counter=0
    until curl -s http://localhost:8080/actuator/health > /dev/null 2>&1 || curl -s http://localhost:8080 > /dev/null 2>&1; do
        sleep 3
        counter=$((counter + 3))
        if [ $counter -ge $timeout ]; then
            print_warning "Application may still be starting. Check logs if needed."
            break
        fi
    done
    
    echo ""
    print_success "🎉 Booky Backend is ready!"
    echo ""
    echo "📋 Service URLs:"
    echo "   🚀 API:          http://localhost:8080"
    echo "   📚 Swagger UI:   http://localhost:8080/swagger-ui/index.html"
    echo "   🗄️  Adminer:      http://localhost:8081"
    echo "   📊 Database:     localhost:5433 (postgres/admin)"
    echo ""
    echo "🧪 Test the API:"
    echo "   curl http://localhost:8080/api/books/search?q=test"
    echo ""
}

# Function to stop the application
stop_app() {
    print_status "Stopping Booky Backend application..."
    docker-compose down
    print_success "Application stopped ✓"
}

# Function to restart the application
restart_app() {
    print_status "Restarting Booky Backend application..."
    docker-compose down
    docker-compose up --build -d
    print_success "Application restarted ✓"
}

# Function to rebuild from scratch
rebuild_app() {
    print_status "Rebuilding Booky Backend from scratch..."
    print_status "Stopping and removing containers, images, and volumes..."
    docker-compose down -v --rmi all --remove-orphans
    print_status "Pruning Docker system..."
    docker system prune -f
    print_status "Building and starting fresh containers..."
    docker-compose up --build -d
    print_success "Application rebuilt from scratch ✓"
}

# Function to rebuild only the app
rebuild_app_only() {
    print_status "Rebuilding only Booky App (keeping database)..."
    print_status "Stopping booky-app container..."
    docker-compose stop booky-app
    print_status "Removing booky-app container..."
    docker-compose rm -f booky-app
    print_status "Rebuilding booky-app image..."
    docker-compose build --no-cache booky-app
    print_status "Starting booky-app container..."
    docker-compose up -d booky-app
    print_success "Booky App rebuilt and restarted ✓"
}

# Function to show logs
show_logs() {
    print_status "Showing application logs (Press Ctrl+C to exit)..."
    docker-compose logs -f booky-app
}

# Function to show database logs
show_db_logs() {
    print_status "Showing database logs (Press Ctrl+C to exit)..."
    docker-compose logs -f postgres
}

# Function to clean up
cleanup() {
    print_status "Cleaning up Docker resources..."
    docker-compose down -v --remove-orphans
    docker system prune -f
    print_success "Cleanup completed ✓"
}

# Function to show status
show_status() {
    print_status "Docker containers status:"
    docker-compose ps
}

# Main menu
show_menu() {
    echo ""
    echo "Select an option:"
    echo "1) 🚀 Start application"
    echo "2) 🛑 Stop application"
    echo "3) 🔄 Restart application"
    echo "4) 🔨 Rebuild from scratch"
    echo "5) ⚡ Rebuild app only (fast)"
    echo "6) 📋 Show status"
    echo "7) 📜 Show app logs"
    echo "8) 🗄️  Show database logs"
    echo "9) 🧹 Clean up"
    echo "10) ❌ Exit"
    echo ""
}

# Check Docker first
check_docker

# If no arguments provided, show menu
if [ $# -eq 0 ]; then
    while true; do
        show_menu
        read -p "Enter your choice [1-10]: " choice
        case $choice in
            1) start_app ;;
            2) stop_app ;;
            3) restart_app ;;
            4) rebuild_app ;;
            5) rebuild_app_only ;;
            6) show_status ;;
            7) show_logs ;;
            8) show_db_logs ;;
            9) cleanup ;;
            10) print_success "Goodbye! 👋"; exit 0 ;;
            *) print_error "Invalid option. Please try again." ;;
        esac
        echo ""
        read -p "Press Enter to continue..."
    done
else
    # Handle command line arguments
    case $1 in
        start) start_app ;;
        stop) stop_app ;;
        restart) restart_app ;;
        rebuild) rebuild_app ;;
        rebuild-app) rebuild_app_only ;;
        status) show_status ;;
        logs) show_logs ;;
        db-logs) show_db_logs ;;
        cleanup) cleanup ;;
        *) 
            echo "Usage: $0 [start|stop|restart|rebuild|rebuild-app|status|logs|db-logs|cleanup]"
            exit 1
            ;;
    esac
fi 