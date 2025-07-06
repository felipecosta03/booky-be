#!/bin/bash

# Script de gestión del servidor Booky Backend
# Facilita operaciones comunes en el servidor EC2

set -e

# Configuración por defecto
EC2_HOST=${EC2_HOST:-""}
EC2_USER=${EC2_USER:-"ubuntu"}
EC2_KEY=${EC2_KEY:-"booky-key.pem"}
APP_DIR="/opt/booky-app"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para mostrar ayuda
show_help() {
    echo "🚀 Booky Backend Server Management"
    echo ""
    echo "Uso: $0 [COMANDO]"
    echo ""
    echo "Comandos disponibles:"
    echo "  connect       - Conectarse al servidor por SSH"
    echo "  status        - Ver estado de los contenedores"
    echo "  logs          - Ver logs de la aplicación"
    echo "  restart       - Reiniciar la aplicación"
    echo "  stop          - Parar todos los contenedores"
    echo "  start         - Iniciar todos los contenedores"
    echo "  update        - Actualizar la aplicación (pull + restart)"
    echo "  backup        - Crear backup de la base de datos"
    echo "  restore       - Restaurar backup de la base de datos"
    echo "  cleanup       - Limpiar contenedores e imágenes no utilizadas"
    echo "  health        - Verificar salud de la aplicación"
    echo "  setup         - Configurar variables de entorno"
    echo "  help          - Mostrar esta ayuda"
    echo ""
    echo "Variables de entorno necesarias:"
    echo "  EC2_HOST      - IP o hostname del servidor"
    echo "  EC2_USER      - Usuario SSH (default: ubuntu)"
    echo "  EC2_KEY       - Ruta al archivo de llave privada (default: booky-key.pem)"
    echo ""
    echo "Ejemplos:"
    echo "  export EC2_HOST=3.123.45.67"
    echo "  $0 connect"
    echo "  $0 status"
    echo "  $0 logs"
}

# Función para verificar configuración
check_config() {
    if [[ -z "$EC2_HOST" ]]; then
        echo -e "${RED}❌ Error: EC2_HOST no está configurado${NC}"
        echo "Configura la variable: export EC2_HOST=tu_ip_publica"
        exit 1
    fi
    
    if [[ ! -f "$EC2_KEY" ]]; then
        echo -e "${RED}❌ Error: Archivo de llave privada no encontrado: $EC2_KEY${NC}"
        echo "Asegúrate de que el archivo existe y tiene los permisos correctos (600)"
        exit 1
    fi
    
    # Verificar permisos del archivo de llave
    if [[ "$(stat -c %a "$EC2_KEY")" != "600" ]]; then
        echo -e "${YELLOW}⚠️  Ajustando permisos del archivo de llave...${NC}"
        chmod 600 "$EC2_KEY"
    fi
}

# Función para ejecutar comandos remotos
remote_exec() {
    ssh -i "$EC2_KEY" -o StrictHostKeyChecking=no "$EC2_USER@$EC2_HOST" "$1"
}

# Función para conectarse al servidor
connect_server() {
    echo -e "${BLUE}🔌 Conectándose al servidor...${NC}"
    ssh -i "$EC2_KEY" -o StrictHostKeyChecking=no "$EC2_USER@$EC2_HOST"
}

# Función para ver estado de contenedores
check_status() {
    echo -e "${BLUE}📊 Verificando estado de los contenedores...${NC}"
    remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml ps"
}

# Función para ver logs
view_logs() {
    echo -e "${BLUE}📋 Mostrando logs de la aplicación...${NC}"
    echo "Presiona Ctrl+C para salir"
    remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml logs -f --tail=50"
}

# Función para reiniciar la aplicación
restart_app() {
    echo -e "${YELLOW}🔄 Reiniciando la aplicación...${NC}"
    remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml restart booky-app"
    echo -e "${GREEN}✅ Aplicación reiniciada${NC}"
}

# Función para parar todos los contenedores
stop_all() {
    echo -e "${YELLOW}⏹️  Parando todos los contenedores...${NC}"
    remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml down"
    echo -e "${GREEN}✅ Contenedores parados${NC}"
}

# Función para iniciar todos los contenedores
start_all() {
    echo -e "${GREEN}▶️  Iniciando todos los contenedores...${NC}"
    remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml up -d"
    echo -e "${GREEN}✅ Contenedores iniciados${NC}"
}

# Función para actualizar la aplicación
update_app() {
    echo -e "${BLUE}🔄 Actualizando la aplicación...${NC}"
    remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml pull"
    remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml up -d --force-recreate"
    echo -e "${GREEN}✅ Aplicación actualizada${NC}"
}

# Función para crear backup de la base de datos
backup_database() {
    echo -e "${BLUE}💾 Creando backup de la base de datos...${NC}"
    BACKUP_NAME="booky-backup-$(date +%Y%m%d_%H%M%S).sql"
    remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml exec -T postgres pg_dump -U postgres booky > backups/$BACKUP_NAME"
    echo -e "${GREEN}✅ Backup creado: $BACKUP_NAME${NC}"
}

# Función para restaurar backup de la base de datos
restore_database() {
    echo -e "${YELLOW}⚠️  Esta operación reemplazará la base de datos actual${NC}"
    echo -e "${BLUE}📋 Backups disponibles:${NC}"
    remote_exec "cd $APP_DIR && ls -la backups/*.sql" || echo "No hay backups disponibles"
    echo ""
    read -p "Ingresa el nombre del backup a restaurar: " BACKUP_FILE
    
    if [[ -n "$BACKUP_FILE" ]]; then
        echo -e "${BLUE}🔄 Restaurando backup: $BACKUP_FILE${NC}"
        remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml exec -T postgres psql -U postgres -c 'DROP DATABASE IF EXISTS booky;'"
        remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml exec -T postgres psql -U postgres -c 'CREATE DATABASE booky;'"
        remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml exec -T postgres psql -U postgres booky < backups/$BACKUP_FILE"
        echo -e "${GREEN}✅ Backup restaurado${NC}"
    else
        echo -e "${RED}❌ No se especificó un archivo de backup${NC}"
    fi
}

# Función para limpiar contenedores e imágenes
cleanup_docker() {
    echo -e "${BLUE}🧹 Limpiando contenedores e imágenes no utilizadas...${NC}"
    remote_exec "docker system prune -f --volumes"
    remote_exec "docker image prune -f"
    echo -e "${GREEN}✅ Limpieza completada${NC}"
}

# Función para verificar salud de la aplicación
health_check() {
    echo -e "${BLUE}❤️  Verificando salud de la aplicación...${NC}"
    
    # Verificar que los contenedores estén corriendo
    if remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml ps | grep -q 'Up'"; then
        echo -e "${GREEN}✅ Contenedores corriendo${NC}"
    else
        echo -e "${RED}❌ Algunos contenedores no están corriendo${NC}"
    fi
    
    # Verificar endpoint de salud
    if remote_exec "curl -f http://localhost:8080/actuator/health > /dev/null 2>&1"; then
        echo -e "${GREEN}✅ Aplicación respondiendo correctamente${NC}"
    else
        echo -e "${RED}❌ Aplicación no responde${NC}"
    fi
    
    # Verificar base de datos
    if remote_exec "cd $APP_DIR && docker-compose -f docker-compose.prod.yml exec -T postgres pg_isready -U postgres -d booky > /dev/null 2>&1"; then
        echo -e "${GREEN}✅ Base de datos conectada${NC}"
    else
        echo -e "${RED}❌ Base de datos no disponible${NC}"
    fi
}

# Función para configurar variables de entorno
setup_env() {
    echo -e "${BLUE}⚙️  Configuración de variables de entorno${NC}"
    echo ""
    echo "Configuración actual:"
    echo "  EC2_HOST: $EC2_HOST"
    echo "  EC2_USER: $EC2_USER"
    echo "  EC2_KEY: $EC2_KEY"
    echo ""
    
    read -p "¿Deseas cambiar la IP del servidor? (actual: $EC2_HOST): " NEW_HOST
    if [[ -n "$NEW_HOST" ]]; then
        export EC2_HOST="$NEW_HOST"
        echo "export EC2_HOST=$NEW_HOST" >> ~/.bashrc
        echo -e "${GREEN}✅ IP actualizada${NC}"
    fi
    
    read -p "¿Deseas cambiar el usuario SSH? (actual: $EC2_USER): " NEW_USER
    if [[ -n "$NEW_USER" ]]; then
        export EC2_USER="$NEW_USER"
        echo "export EC2_USER=$NEW_USER" >> ~/.bashrc
        echo -e "${GREEN}✅ Usuario actualizado${NC}"
    fi
    
    read -p "¿Deseas cambiar la ruta de la llave privada? (actual: $EC2_KEY): " NEW_KEY
    if [[ -n "$NEW_KEY" ]]; then
        export EC2_KEY="$NEW_KEY"
        echo "export EC2_KEY=$NEW_KEY" >> ~/.bashrc
        echo -e "${GREEN}✅ Ruta de llave actualizada${NC}"
    fi
}

# Función principal
main() {
    case "${1:-help}" in
        connect)
            check_config
            connect_server
            ;;
        status)
            check_config
            check_status
            ;;
        logs)
            check_config
            view_logs
            ;;
        restart)
            check_config
            restart_app
            ;;
        stop)
            check_config
            stop_all
            ;;
        start)
            check_config
            start_all
            ;;
        update)
            check_config
            update_app
            ;;
        backup)
            check_config
            backup_database
            ;;
        restore)
            check_config
            restore_database
            ;;
        cleanup)
            check_config
            cleanup_docker
            ;;
        health)
            check_config
            health_check
            ;;
        setup)
            setup_env
            ;;
        help|*)
            show_help
            ;;
    esac
}

# Ejecutar función principal
main "$@" 