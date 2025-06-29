#!/bin/bash

# Script para dar de alta usuarios usando la API REST de Booky
# Uso: ./create_user.sh [API_URL]

# Configuración
API_URL=${1:-"http://localhost:8080"}
SIGNUP_ENDPOINT="$API_URL/sign-up"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Función para mostrar ayuda
show_help() {
    echo -e "${BLUE}=== Script de Alta de Usuarios - Booky ===${NC}"
    echo ""
    echo "Uso: $0 [URL_API]"
    echo ""
    echo "Opciones:"
    echo "  URL_API    URL base de la API (default: http://localhost:8080)"
    echo ""
    echo "Ejemplos:"
    echo "  $0                                    # Usar localhost:8080"
    echo "  $0 https://api.booky.com             # Usar API remota"
    echo ""
    echo "El script permite crear usuarios de forma interactiva o masiva."
}

# Función para validar email
validate_email() {
    local email=$1
    if [[ $email =~ ^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$ ]]; then
        return 0
    else
        return 1
    fi
}

# Función para crear un usuario
create_user() {
    local name=$1
    local lastname=$2
    local email=$3
    local username=$4
    local password=$5

    # Crear JSON payload
    local json_payload=$(cat <<EOF
{
    "name": "$name",
    "lastname": "$lastname", 
    "email": "$email",
    "username": "$username",
    "password": "$password"
}
EOF
)

    echo -e "${YELLOW}Creando usuario: $username ($email)...${NC}"
    
    # Hacer petición POST
    response=$(curl -s -w "HTTPSTATUS:%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$json_payload" \
        "$SIGNUP_ENDPOINT")

    # Extraer status code
    http_code=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    body=$(echo $response | sed -e 's/HTTPSTATUS\:.*//g')

    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓ Usuario creado exitosamente${NC}"
        echo -e "${BLUE}Respuesta:${NC} $body"
        return 0
    else
        echo -e "${RED}✗ Error al crear usuario (HTTP $http_code)${NC}"
        echo -e "${RED}Respuesta:${NC} $body"
        return 1
    fi
}

# Función para crear usuario interactivo
create_user_interactive() {
    echo -e "${BLUE}=== Crear Nuevo Usuario ===${NC}"
    
    # Solicitar datos
    read -p "Nombre: " name
    read -p "Apellido: " lastname
    
    while true; do
        read -p "Email: " email
        if validate_email "$email"; then
            break
        else
            echo -e "${RED}Email inválido. Intenta nuevamente.${NC}"
        fi
    done
    
    read -p "Nombre de usuario: " username
    
    while true; do
        read -s -p "Contraseña: " password
        echo
        read -s -p "Confirmar contraseña: " password_confirm
        echo
        
        if [ "$password" = "$password_confirm" ]; then
            if [ ${#password} -lt 6 ]; then
                echo -e "${RED}La contraseña debe tener al menos 6 caracteres.${NC}"
            else
                break
            fi
        else
            echo -e "${RED}Las contraseñas no coinciden.${NC}"
        fi
    done

    # Crear usuario
    create_user "$name" "$lastname" "$email" "$username" "$password"
}

# Función para crear usuarios masivos desde archivo
create_users_from_file() {
    local file=$1
    
    if [ ! -f "$file" ]; then
        echo -e "${RED}Error: El archivo $file no existe${NC}"
        return 1
    fi

    echo -e "${BLUE}Creando usuarios desde archivo: $file${NC}"
    
    local success_count=0
    local error_count=0
    
    # Leer archivo línea por línea (formato: name,lastname,email,username,password)
    while IFS=',' read -r name lastname email username password; do
        # Saltar líneas vacías o comentarios
        [[ -z "$name" || "$name" =~ ^#.* ]] && continue
        
        if create_user "$name" "$lastname" "$email" "$username" "$password"; then
            ((success_count++))
        else
            ((error_count++))
        fi
        
        # Pausa pequeña entre requests
        sleep 1
    done < "$file"
    
    echo -e "${BLUE}=== Resumen ===${NC}"
    echo -e "${GREEN}Usuarios creados exitosamente: $success_count${NC}"
    echo -e "${RED}Errores: $error_count${NC}"
}

# Función para crear usuarios de prueba
create_test_users() {
    echo -e "${BLUE}Creando usuarios de prueba...${NC}"
    
    # Array de usuarios de prueba
    declare -a test_users=(
        "Carlos,Mendez,carlos.mendez@test.com,carlosm,password123"
        "Maria,Rodriguez,maria.rodriguez@test.com,mariar,password123"
        "Luis,Garcia,luis.garcia@test.com,luisg,password123"
        "Ana,Martinez,ana.martinez@test.com,anam,password123"
        "Pedro,Sanchez,pedro.sanchez@test.com,pedros,password123"
    )
    
    local success_count=0
    local error_count=0
    
    for user_data in "${test_users[@]}"; do
        IFS=',' read -r name lastname email username password <<< "$user_data"
        
        if create_user "$name" "$lastname" "$email" "$username" "$password"; then
            ((success_count++))
        else
            ((error_count++))
        fi
        
        sleep 1
    done
    
    echo -e "${BLUE}=== Resumen ===${NC}"
    echo -e "${GREEN}Usuarios de prueba creados: $success_count${NC}"
    echo -e "${RED}Errores: $error_count${NC}"
}

# Menú principal
main_menu() {
    while true; do
        echo -e "\n${BLUE}=== Booky - Alta de Usuarios ===${NC}"
        echo -e "API URL: ${YELLOW}$API_URL${NC}"
        echo ""
        echo "1. Crear usuario interactivo"
        echo "2. Crear usuarios desde archivo CSV"
        echo "3. Crear usuarios de prueba"
        echo "4. Cambiar URL de API"
        echo "5. Ayuda"
        echo "6. Salir"
        echo ""
        read -p "Selecciona una opción (1-6): " choice
        
        case $choice in
            1)
                create_user_interactive
                ;;
            2)
                read -p "Ingresa la ruta del archivo CSV: " file_path
                create_users_from_file "$file_path"
                ;;
            3)
                create_test_users
                ;;
            4)
                read -p "Ingresa la nueva URL de API: " new_url
                API_URL=$new_url
                SIGNUP_ENDPOINT="$API_URL/sign-up"
                echo -e "${GREEN}URL actualizada a: $API_URL${NC}"
                ;;
            5)
                show_help
                ;;
            6)
                echo -e "${GREEN}¡Hasta luego!${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}Opción inválida${NC}"
                ;;
        esac
    done
}

# Verificar argumentos
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
    exit 0
fi

# Verificar que curl está instalado
if ! command -v curl &> /dev/null; then
    echo -e "${RED}Error: curl no está instalado${NC}"
    exit 1
fi

# Verificar conectividad con la API
echo -e "${YELLOW}Verificando conectividad con la API...${NC}"
if curl -s --connect-timeout 5 "$API_URL" > /dev/null; then
    echo -e "${GREEN}✓ API accesible${NC}"
else
    echo -e "${RED}⚠ Advertencia: No se puede conectar a la API en $API_URL${NC}"
    echo -e "${YELLOW}Asegúrate de que el servidor esté ejecutándose${NC}"
fi

# Iniciar menú principal
main_menu 