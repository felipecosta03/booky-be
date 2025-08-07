#!/bin/bash

# Script para crear userbooks de prueba
# Uso: ./create_userbooks.sh [BASE_URL] [USER_ID]
# Ejemplo: ./create_userbooks.sh http://localhost:8080 user-123

BASE_URL=${1:-"http://localhost:8080"}
USER_ID=${2:-"user-123"}

echo "==========================================="
echo "Script de Alta de UserBooks"
echo "==========================================="
echo "Base URL: $BASE_URL"
echo "User ID: $USER_ID"
echo "==========================================="

# Lista de libros de ejemplo con ISBN y status
declare -a BOOKS=(
    "9780141439518,READ"           # Pride and Prejudice
    "9780446310789,READING"        # To Kill a Mockingbird  
    "9780743273565,WISHLIST"       # The Great Gatsby
    "9780451524935,TO_READ"        # 1984
    "9780060935467,READ"           # To Kill a Mockingbird (Harper Lee)
    "9780553380163,READING"        # Brave New World
    "9780486282114,WISHLIST"       # The Picture of Dorian Gray
    "9780140449136,TO_READ"        # Crime and Punishment
    "9780062315007,READ"           # The Alchemist
    "9780307887436,READING"        # The Girl with the Dragon Tattoo
    "9780316769174,WISHLIST"       # The Catcher in the Rye
    "9780452284234,TO_READ"        # Animal Farm
    "9780375760891,READ"           # The Book Thief
    "9780385537859,READING"        # The Fault in Our Stars
    "9780439708180,WISHLIST"       # Harry Potter and the Sorcerer's Stone
)

# Contador para tracking
TOTAL=${#BOOKS[@]}
SUCCESS=0
FAILED=0

echo "Iniciando la creación de $TOTAL userbooks..."
echo ""

# Función para crear un userbook
create_userbook() {
    local isbn=$1
    local status=$2
    local book_title=$3
    
    echo "📚 Agregando libro: $book_title"
    echo "   ISBN: $isbn | Status: $status"
    
    # Preparar el JSON payload
    local json_payload=$(cat <<EOF
{
    "isbn": "$isbn",
    "status": "$status"
}
EOF
)
    
    # Hacer la request
    local response=$(curl -s -w "\n%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d "$json_payload" \
        "$BASE_URL/books/users/$USER_ID/library")
    
    # Separar response body y status code
    local http_code=$(echo "$response" | tail -n1)
    local response_body=$(echo "$response" | head -n -1)
    
    # Evaluar resultado
    if [ "$http_code" = "200" ]; then
        echo "   ✅ SUCCESS - Libro agregado exitosamente"
        ((SUCCESS++))
    elif [ "$http_code" = "409" ]; then
        echo "   ⚠️  CONFLICT - El libro ya existe en la biblioteca del usuario"
        ((SUCCESS++))
    else
        echo "   ❌ ERROR - HTTP $http_code"
        echo "   Response: $response_body"
        ((FAILED++))
    fi
    
    echo ""
}

# Procesar cada libro
for book_data in "${BOOKS[@]}"; do
    IFS=',' read -r isbn status <<< "$book_data"
    
    # Obtener información del libro (opcional, para mostrar título)
    book_info=$(curl -s "$BASE_URL/books/isbn/$isbn")
    book_title=$(echo "$book_info" | grep -o '"title":"[^"]*"' | cut -d'"' -f4)
    
    if [ -z "$book_title" ]; then
        book_title="Libro con ISBN $isbn"
    fi
    
    create_userbook "$isbn" "$status" "$book_title"
    
    # Pequeña pausa para no sobrecargar el servidor
    sleep 0.5
done

echo "==========================================="
echo "RESUMEN DE EJECUCIÓN"
echo "==========================================="
echo "Total de libros procesados: $TOTAL"
echo "Exitosos: $SUCCESS"
echo "Fallidos: $FAILED"
echo "==========================================="

if [ $FAILED -eq 0 ]; then
    echo "🎉 ¡Todos los libros fueron procesados exitosamente!"
else
    echo "⚠️  Algunos libros no pudieron ser agregados. Revisa los errores anteriores."
fi

echo ""
echo "Para verificar la biblioteca del usuario, ejecuta:"
echo "curl -X GET '$BASE_URL/books/users/$USER_ID/library'"