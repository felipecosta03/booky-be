#!/bin/bash

# Script para configurar la base de datos en EC2 con todos los scripts necesarios

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}🗄️ Configurando base de datos con scripts de inicialización...${NC}"

# Crear directorio de scripts
mkdir -p ~/booky-backend/scripts
cd ~/booky-backend

# Crear script de esquema de base de datos
echo -e "${YELLOW}📝 Creando script de esquema de base de datos...${NC}"
cat > scripts/database_schema_updated.sql << 'EOF'
-- Script SQL Actualizado para Booky Backend
-- Basado en las entidades reales del código fuente

-- =====================================================
-- TABLA: addresses
-- =====================================================
CREATE TABLE addresses (
    id VARCHAR(255) PRIMARY KEY,
    state VARCHAR(255),
    country VARCHAR(255),
    longitude DOUBLE PRECISION,
    latitude DOUBLE PRECISION
);

-- =====================================================
-- TABLA: users 
-- =====================================================
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    username VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    lastname VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    image VARCHAR(255),
    coins INTEGER,
    address_id VARCHAR(255),
    date_created TIMESTAMP,
    FOREIGN KEY (address_id) REFERENCES addresses(id)
);

-- =====================================================
-- TABLA: user_follows (sistema de seguimiento)
-- =====================================================
CREATE TABLE user_follows (
    follower_id VARCHAR(255),
    followed_id VARCHAR(255),
    PRIMARY KEY (follower_id, followed_id),
    FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (followed_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- TABLA: books
-- =====================================================
CREATE TABLE books (
    id VARCHAR(255) PRIMARY KEY,
    isbn VARCHAR(255) UNIQUE,
    title VARCHAR(1000),
    author VARCHAR(255),
    synopsis TEXT,
    overview TEXT,
    pages INTEGER,
    publisher VARCHAR(255),
    edition VARCHAR(255),
    rate DOUBLE PRECISION,
    image VARCHAR(255)
);

-- =====================================================
-- TABLA: book_categories
-- =====================================================
CREATE TABLE book_categories (
    id VARCHAR(255) PRIMARY KEY,
    book_id VARCHAR(255),
    category VARCHAR(255),
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- =====================================================
-- TABLA: community
-- =====================================================
CREATE TABLE community (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image VARCHAR(255),
    date_created TIMESTAMP
);

-- =====================================================
-- TABLA: community_members
-- =====================================================
CREATE TABLE community_members (
    community_id VARCHAR(255),
    user_id VARCHAR(255),
    PRIMARY KEY (community_id, user_id),
    FOREIGN KEY (community_id) REFERENCES community(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- TABLA: reading_clubs
-- =====================================================
CREATE TABLE reading_clubs (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    image VARCHAR(255),
    community_id VARCHAR(255),
    book_id VARCHAR(255),
    date_created TIMESTAMP,
    FOREIGN KEY (community_id) REFERENCES community(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- =====================================================
-- TABLA: reading_club_members
-- =====================================================
CREATE TABLE reading_club_members (
    reading_club_id VARCHAR(255),
    user_id VARCHAR(255),
    PRIMARY KEY (reading_club_id, user_id),
    FOREIGN KEY (reading_club_id) REFERENCES reading_clubs(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- TABLA: user_books
-- =====================================================
CREATE TABLE user_books (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    book_id VARCHAR(255),
    status VARCHAR(50),
    exchange_preference BOOLEAN,
    date_created TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- =====================================================
-- TABLA: post
-- =====================================================
CREATE TABLE post (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255),
    community_id VARCHAR(255),
    content TEXT,
    date_created TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (community_id) REFERENCES community(id) ON DELETE CASCADE
);

-- =====================================================
-- TABLA: comment
-- =====================================================
CREATE TABLE comment (
    id VARCHAR(255) PRIMARY KEY,
    post_id VARCHAR(255),
    user_id VARCHAR(255),
    content TEXT,
    date_created TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES post(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- =====================================================
-- ÍNDICES
-- =====================================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_post_user_id ON post(user_id);
CREATE INDEX idx_post_community_id ON post(community_id);
CREATE INDEX idx_comment_post_id ON comment(post_id);
CREATE INDEX idx_comment_user_id ON comment(user_id);
EOF

# Crear script de datos de usuarios
echo -e "${YELLOW}👥 Creando script de datos de usuarios...${NC}"
cat > scripts/alta_usuarios.sql << 'EOF'
-- Datos de prueba para usuarios

-- Primero insertamos algunas direcciones
INSERT INTO addresses (id, state, country, longitude, latitude) VALUES
('addr1', 'Buenos Aires', 'Argentina', -58.3816, -34.6037),
('addr2', 'Córdoba', 'Argentina', -64.1810, -31.4201),
('addr3', 'Rosario', 'Argentina', -60.6393, -32.9442),
('addr4', 'Mendoza', 'Argentina', -68.8458, -32.8908),
('addr5', 'La Plata', 'Argentina', -57.9545, -34.9214);

-- Insertar usuarios de prueba
INSERT INTO users (id, email, username, password, name, lastname, description, image, coins, address_id, date_created) VALUES
('user1', 'juan.perez@example.com', 'juanp', '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', 'Juan', 'Pérez', 'Amante de la literatura clásica', 'https://via.placeholder.com/150', 50, 'addr1', NOW()),
('user2', 'maria.gonzalez@example.com', 'mariag', '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', 'María', 'González', 'Lectora voraz de ficción', 'https://via.placeholder.com/150', 75, 'addr2', NOW()),
('user3', 'carlos.rodriguez@example.com', 'carlosr', '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', 'Carlos', 'Rodríguez', 'Especialista en ciencia ficción', 'https://via.placeholder.com/150', 30, 'addr3', NOW()),
('user4', 'ana.martinez@example.com', 'anam', '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', 'Ana', 'Martínez', 'Apasionada por el misterio', 'https://via.placeholder.com/150', 100, 'addr4', NOW()),
('user5', 'luis.fernandez@example.com', 'luisf', '$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ', 'Luis', 'Fernández', 'Coleccionista de libros antiguos', 'https://via.placeholder.com/150', 25, 'addr5', NOW());

-- Insertar algunas relaciones de seguimiento
INSERT INTO user_follows (follower_id, followed_id) VALUES
('user1', 'user2'),
('user1', 'user3'),
('user2', 'user1'),
('user2', 'user4'),
('user3', 'user1'),
('user3', 'user5'),
('user4', 'user2'),
('user4', 'user5'),
('user5', 'user3'),
('user5', 'user4');
EOF

# Crear script de datos de libros
echo -e "${YELLOW}📚 Creando script de datos de libros...${NC}"
cat > scripts/alta_libros.sql << 'EOF'
-- Datos de prueba para libros

INSERT INTO books (id, isbn, title, author, synopsis, overview, pages, publisher, edition, rate, image) VALUES
('book1', '978-84-376-0494-7', 'Cien años de soledad', 'Gabriel García Márquez', 'Una obra maestra del realismo mágico', 'La historia de la familia Buendía en Macondo', 471, 'Sudamericana', '1ra edición', 4.8, 'https://via.placeholder.com/200x300'),
('book2', '978-84-204-8207-7', 'Don Quijote de la Mancha', 'Miguel de Cervantes', 'Las aventuras del ingenioso hidalgo', 'Clásico de la literatura española', 1023, 'Alianza Editorial', '2da edición', 4.6, 'https://via.placeholder.com/200x300'),
('book3', '978-84-663-0016-7', 'El Aleph', 'Jorge Luis Borges', 'Cuentos fantásticos y laberintos', 'Colección de relatos borgeanos', 203, 'Emecé', '1ra edición', 4.9, 'https://via.placeholder.com/200x300'),
('book4', '978-84-376-2072-5', 'Rayuela', 'Julio Cortázar', 'Novela experimental argentina', 'Una obra que se puede leer de múltiples maneras', 635, 'Sudamericana', '1ra edición', 4.7, 'https://via.placeholder.com/200x300'),
('book5', '978-84-322-1742-1', 'Ficciones', 'Jorge Luis Borges', 'Cuentos laberínticos', 'Otra colección magistral de Borges', 156, 'Alianza Editorial', '3ra edición', 4.8, 'https://via.placeholder.com/200x300');

-- Insertar categorías de libros
INSERT INTO book_categories (id, book_id, category) VALUES
('cat1', 'book1', 'Realismo Mágico'),
('cat2', 'book1', 'Literatura Latinoamericana'),
('cat3', 'book2', 'Clásicos'),
('cat4', 'book2', 'Literatura Española'),
('cat5', 'book3', 'Fantasía'),
('cat6', 'book3', 'Cuentos'),
('cat7', 'book4', 'Literatura Experimental'),
('cat8', 'book4', 'Literatura Argentina'),
('cat9', 'book5', 'Cuentos'),
('cat10', 'book5', 'Filosofía');
EOF

# Crear script de datos de comunidades
echo -e "${YELLOW}🏘️ Creando script de datos de comunidades...${NC}"
cat > scripts/alta_comunidades.sql << 'EOF'
-- Datos de prueba para comunidades

INSERT INTO community (id, name, description, image, date_created) VALUES
('comm1', 'Lectores de Clásicos', 'Comunidad dedicada a la lectura de obras clásicas de la literatura universal', 'https://via.placeholder.com/300', NOW()),
('comm2', 'Amantes de la Fantasía', 'Espacio para compartir y discutir libros de fantasía y ciencia ficción', 'https://via.placeholder.com/300', NOW()),
('comm3', 'Literatura Latinoamericana', 'Comunidad enfocada en autores y obras de América Latina', 'https://via.placeholder.com/300', NOW()),
('comm4', 'Club de Misterio', 'Para los apasionados del suspenso y el misterio', 'https://via.placeholder.com/300', NOW()),
('comm5', 'Biblioteca Digital', 'Intercambio de recomendaciones y recursos literarios', 'https://via.placeholder.com/300', NOW());

-- Insertar miembros de comunidades
INSERT INTO community_members (community_id, user_id) VALUES
('comm1', 'user1'),
('comm1', 'user2'),
('comm1', 'user4'),
('comm2', 'user3'),
('comm2', 'user5'),
('comm3', 'user1'),
('comm3', 'user2'),
('comm3', 'user3'),
('comm4', 'user4'),
('comm4', 'user5'),
('comm5', 'user1'),
('comm5', 'user2'),
('comm5', 'user3'),
('comm5', 'user4'),
('comm5', 'user5');
EOF

# Crear script de datos de clubes de lectura
echo -e "${YELLOW}📖 Creando script de datos de clubes de lectura...${NC}"
cat > scripts/alta_clubes_lectura.sql << 'EOF'
-- Datos de prueba para clubes de lectura

INSERT INTO reading_clubs (id, name, description, image, community_id, book_id, date_created) VALUES
('club1', 'Club García Márquez', 'Dedicado a la obra del Nobel colombiano', 'https://via.placeholder.com/300', 'comm3', 'book1', NOW()),
('club2', 'Cervantes y su época', 'Explorando el Siglo de Oro español', 'https://via.placeholder.com/300', 'comm1', 'book2', NOW()),
('club3', 'Laberintos Borgeanos', 'Desentrañando la obra de Borges', 'https://via.placeholder.com/300', 'comm3', 'book3', NOW()),
('club4', 'Rayuela interactiva', 'Leyendo Cortázar de manera no lineal', 'https://via.placeholder.com/300', 'comm3', 'book4', NOW());

-- Insertar miembros de clubes de lectura
INSERT INTO reading_club_members (reading_club_id, user_id) VALUES
('club1', 'user1'),
('club1', 'user2'),
('club1', 'user3'),
('club2', 'user1'),
('club2', 'user4'),
('club3', 'user2'),
('club3', 'user3'),
('club3', 'user5'),
('club4', 'user1'),
('club4', 'user5');
EOF

# Crear script de libros de usuarios
echo -e "${YELLOW}📚 Creando script de libros de usuarios...${NC}"
cat > scripts/alta_user_books.sql << 'EOF'
-- Datos de prueba para libros de usuarios

INSERT INTO user_books (id, user_id, book_id, status, exchange_preference, date_created) VALUES
('ub1', 'user1', 'book1', 'READ', true, NOW()),
('ub2', 'user1', 'book2', 'READING', false, NOW()),
('ub3', 'user2', 'book1', 'WANT_TO_READ', true, NOW()),
('ub4', 'user2', 'book3', 'READ', true, NOW()),
('ub5', 'user3', 'book2', 'READING', false, NOW()),
('ub6', 'user3', 'book4', 'READ', true, NOW()),
('ub7', 'user4', 'book3', 'WANT_TO_READ', false, NOW()),
('ub8', 'user4', 'book5', 'READ', true, NOW()),
('ub9', 'user5', 'book1', 'READING', false, NOW()),
('ub10', 'user5', 'book4', 'READ', true, NOW());
EOF

# Actualizar docker-compose.yml para incluir todos los scripts
echo -e "${YELLOW}🐳 Actualizando docker-compose.yml con todos los scripts...${NC}"
cat > docker-compose.yml << 'EOF'
services:
  postgres:
    image: postgres:15-alpine
    container_name: booky-postgres-prod
    environment:
      POSTGRES_DB: booky
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-admin}
      PGDATA: /var/lib/postgresql/data/pgdata
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./scripts/database_schema_updated.sql:/docker-entrypoint-initdb.d/00-schema.sql
      - ./scripts/alta_usuarios.sql:/docker-entrypoint-initdb.d/01-users.sql
      - ./scripts/alta_libros.sql:/docker-entrypoint-initdb.d/02-books.sql
      - ./scripts/alta_comunidades.sql:/docker-entrypoint-initdb.d/03-communities.sql
      - ./scripts/alta_clubes_lectura.sql:/docker-entrypoint-initdb.d/04-reading-clubs.sql
      - ./scripts/alta_user_books.sql:/docker-entrypoint-initdb.d/05-user-books.sql
    networks:
      - booky-network
    restart: unless-stopped
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres -d booky"]
      interval: 30s
      timeout: 10s
      retries: 3

  booky-app:
    image: bookypfi/booky-backend:latest
    container_name: booky-backend-prod
    environment:
      DATABASE_URL: jdbc:postgresql://postgres:5432/booky
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: ${POSTGRES_PASSWORD:-admin}
      DATABASE_NAME: booky
      SPRING_PROFILES_ACTIVE: production
      SERVER_PORT: 8080
      JWT_SECRET: ${JWT_SECRET:-your-secret-key-here}
    ports:
      - "8080:8080"
    networks:
      - booky-network
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy

  adminer:
    image: adminer:4.8.1
    container_name: booky-adminer
    ports:
      - "8081:8080"
    networks:
      - booky-network
    restart: unless-stopped
    depends_on:
      - postgres

volumes:
  postgres_data:
    driver: local

networks:
  booky-network:
    driver: bridge
EOF

# Crear archivo .env si no existe
if [ ! -f .env ]; then
    echo -e "${YELLOW}🔧 Creando archivo .env...${NC}"
    cat > .env << 'EOF'
POSTGRES_PASSWORD=admin
JWT_SECRET=booky_jwt_secret_key_very_secure_32_chars_minimum
EOF
fi

# Parar y eliminar containers actuales (incluyendo volúmenes)
echo -e "${YELLOW}🛑 Parando containers actuales...${NC}"
docker-compose down -v

# Esperar un momento
sleep 5

# Iniciar servicios nuevamente
echo -e "${YELLOW}🚀 Iniciando servicios con datos de prueba...${NC}"
docker-compose up -d

# Esperar a que PostgreSQL esté listo
echo -e "${YELLOW}⏳ Esperando a que PostgreSQL esté listo...${NC}"
sleep 30

# Verificar que los servicios estén corriendo
echo -e "${YELLOW}📊 Verificando servicios...${NC}"
docker-compose ps

# Verificar datos
echo -e "${YELLOW}🔍 Verificando datos en la base de datos...${NC}"
echo "Usuarios:"
docker exec -it booky-postgres-prod psql -U postgres -d booky -c "SELECT COUNT(*) as total_users FROM users;"

echo "Libros:"
docker exec -it booky-postgres-prod psql -U postgres -d booky -c "SELECT COUNT(*) as total_books FROM books;"

echo "Comunidades:"
docker exec -it booky-postgres-prod psql -U postgres -d booky -c "SELECT COUNT(*) as total_communities FROM community;"

echo "Clubes de lectura:"
docker exec -it booky-postgres-prod psql -U postgres -d booky -c "SELECT COUNT(*) as total_reading_clubs FROM reading_clubs;"

echo -e "${GREEN}✅ Base de datos configurada con datos de prueba!${NC}"
echo -e "${GREEN}🌐 Acceso a Adminer: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8081${NC}"
echo -e "${GREEN}🌐 Acceso a la API: http://$(curl -s http://169.254.169.254/latest/meta-data/public-ipv4):8080${NC}"

echo -e "${YELLOW}📝 Credenciales para Adminer:${NC}"
echo "System: PostgreSQL"
echo "Server: postgres"
echo "Username: postgres"
echo "Password: admin"
echo "Database: booky" 