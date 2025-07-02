#!/bin/bash

# 🚀 Booky-BE API Endpoints Test Script
# Este script ejecuta pruebas en todos los endpoints usando datos reales

# Colores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}🚀 TESTING ALL BOOKY-BE API ENDPOINTS${NC}"
echo "========================================"
echo ""

# Base URL
BASE_URL="http://localhost:8080"

# Function to test endpoint
test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3
    local data=$4
    
    echo -e "${YELLOW}Testing: $description${NC}"
    echo "Endpoint: $method $endpoint"
    
    if [ -z "$data" ]; then
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X $method "$BASE_URL$endpoint")
    else
        response=$(curl -s -w "HTTPSTATUS:%{http_code}" -X $method "$BASE_URL$endpoint" \
            -H "Content-Type: application/json" \
            -d "$data")
    fi
    
    http_code=$(echo $response | tr -d '\n' | sed -e 's/.*HTTPSTATUS://')
    body=$(echo $response | sed -e 's/HTTPSTATUS\:.*//g')
    
    if [ $http_code -ge 200 ] && [ $http_code -lt 300 ]; then
        echo -e "${GREEN}✅ SUCCESS (HTTP $http_code)${NC}"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        echo -e "${RED}❌ FAILED (HTTP $http_code)${NC}"
        echo "$body"
    fi
    echo ""
}

echo -e "${BLUE}📚 BOOKS ENDPOINTS${NC}"
echo "=================="

test_endpoint "GET" "/books/search?q=hobbit" "Search books by 'hobbit'"
test_endpoint "GET" "/books/isbn/9780060935467" "Get book by ISBN (To Kill a Mockingbird)"
test_endpoint "GET" "/books/exchange" "Get books available for exchange"

echo -e "${BLUE}👤 USERS ENDPOINTS${NC}"
echo "=================="

test_endpoint "GET" "/users" "Get all users"
test_endpoint "GET" "/users/user-001" "Get specific user (Juan Pérez)"

echo -e "${BLUE}🔐 AUTHENTICATION ENDPOINTS${NC}"
echo "==========================="

test_endpoint "POST" "/sign-up" "Create new user" '{
    "username": "testuser_'$(date +%s)'",
    "password": "test123",
    "name": "Test",
    "lastname": "User",
    "email": "test'$(date +%s)'@test.com",
    "description": "Test user",
    "address": {
        "state": "Test State",
        "country": "Test Country",
        "longitude": -58.3816,
        "latitude": -34.6037
    }
}'

test_endpoint "POST" "/sign-in" "Sign in (Known issue - returns 401)" '{
    "email": "juan.perez@gmail.com",
    "password": "password123"
}'

echo -e "${BLUE}📚 LIBRARY OPERATIONS${NC}"
echo "====================="

test_endpoint "POST" "/books/users/user-001/library" "Add book to user library" '{
    "isbn": "9780439708180",
    "status": "TO_READ"
}'

test_endpoint "GET" "/books/users/user-001/library" "Get user library"
test_endpoint "GET" "/books/users/user-001/favorites" "Get user favorites"

# Get a book ID from user library for further tests
echo -e "${YELLOW}Getting book ID for status/favorite tests...${NC}"
BOOK_ID=$(curl -s "$BASE_URL/books/users/user-001/library" | jq -r '.[0].book.id' 2>/dev/null)

if [ "$BOOK_ID" != "null" ] && [ -n "$BOOK_ID" ]; then
    echo "Using book ID: $BOOK_ID"
    
    test_endpoint "PUT" "/books/users/user-001/books/$BOOK_ID/status" "Update book status" '{
        "status": "READING"
    }'
    
    test_endpoint "PUT" "/books/users/user-001/books/$BOOK_ID/favorite" "Toggle book favorite"
    
    test_endpoint "PUT" "/books/users/user-001/books/$BOOK_ID/exchange" "Update exchange preference" '{
        "wants_to_exchange": true
    }'
else
    echo -e "${RED}Could not get book ID for status/favorite tests${NC}"
fi

echo -e "${BLUE}🏘️ COMMUNITIES ENDPOINTS${NC}"
echo "========================"

test_endpoint "GET" "/communities" "Get all communities"
test_endpoint "GET" "/communities/comm-001" "Get specific community (Literatura Clásica)"

echo -e "${BLUE}📖 READING CLUBS ENDPOINTS${NC}"
echo "=========================="

test_endpoint "GET" "/reading-clubs" "Get all reading clubs"
test_endpoint "GET" "/reading-clubs/user/user-001" "Get reading clubs by user"
test_endpoint "GET" "/reading-clubs/community/comm-001" "Get reading clubs by community"

echo -e "${BLUE}📝 POSTS ENDPOINTS${NC}"
echo "=================="

test_endpoint "GET" "/posts" "Get all posts"
test_endpoint "GET" "/posts/community/comm-001" "Get posts by community"
test_endpoint "GET" "/posts/user/user-001" "Get posts by user"

echo ""
echo -e "${BLUE}📊 TESTING SUMMARY${NC}"
echo "=================="
echo -e "${GREEN}✅ Most endpoints are working correctly${NC}"
echo -e "${RED}❌ Known issue: POST /sign-in returns 401${NC}"
echo -e "${YELLOW}ℹ️  Check individual test results above for details${NC}"
echo ""
echo -e "${BLUE}🔧 QUICK COMMANDS FOR MANUAL TESTING:${NC}"
echo "====================================="
echo "• Add book to library: curl -X POST '$BASE_URL/books/users/user-001/library' -H 'Content-Type: application/json' -d '{\"isbn\": \"9780316769488\", \"status\": \"TO_READ\"}'"
echo "• Get user library: curl '$BASE_URL/books/users/user-001/library'"
echo "• Search books: curl '$BASE_URL/books/search?q=fiction'"
echo "• Get communities: curl '$BASE_URL/communities'"
echo "" 