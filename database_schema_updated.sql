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
    id BIGSERIAL PRIMARY KEY,
    isbn VARCHAR(255),
    title VARCHAR(255),
    overview TEXT,
    synopsis TEXT,
    pages INTEGER,
    edition VARCHAR(255),
    publisher VARCHAR(255),
    author VARCHAR(255),
    image VARCHAR(255),
    rate INTEGER,
    status VARCHAR(255)
);

-- =====================================================
-- TABLA: book_categories (relación libros-categorías)
-- =====================================================
CREATE TABLE book_categories (
    book_id BIGINT,
    category VARCHAR(255),
    PRIMARY KEY (book_id, category),
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- =====================================================
-- TABLAS ADICIONALES DEL DIAGRAMA ERD
-- (Manteniendo la estructura del diagrama para futuras funcionalidades)
-- =====================================================

-- Tabla: level (sistema de niveles)
CREATE TABLE level (
    id VARCHAR(255) PRIMARY KEY,
    min_points INTEGER,
    name VARCHAR(255)
);

-- Tabla: user_activity (actividad del usuario)
CREATE TABLE user_activity (
    id VARCHAR(255) PRIMARY KEY,
    points INTEGER,
    level_id VARCHAR(255),
    FOREIGN KEY (level_id) REFERENCES level(id)
);

-- Tabla: badge (insignias)
CREATE TABLE badge (
    id VARCHAR(255) PRIMARY KEY,
    description VARCHAR(255),
    image_url VARCHAR(255),
    name VARCHAR(255)
);

-- Tabla: user_activity_badges (relación actividad-insignias)
CREATE TABLE user_activity_badges (
    user_activity_id VARCHAR(255),
    badge_id VARCHAR(255),
    PRIMARY KEY (user_activity_id, badge_id),
    FOREIGN KEY (user_activity_id) REFERENCES user_activity(id),
    FOREIGN KEY (badge_id) REFERENCES badge(id)
);

-- Tabla: author (autores de libros)
CREATE TABLE author (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255)
);

-- Tabla: user_rates (calificaciones entre usuarios)
CREATE TABLE user_rates (
    id VARCHAR(255) PRIMARY KEY,
    comment VARCHAR(255),
    date_created TIMESTAMP,
    rate INTEGER,
    rated_user_id VARCHAR(255),
    rater_user_id VARCHAR(255),
    FOREIGN KEY (rated_user_id) REFERENCES users(id),
    FOREIGN KEY (rater_user_id) REFERENCES users(id)
);

-- Tabla: community (comunidades)
CREATE TABLE community (
    id VARCHAR(255) PRIMARY KEY,
    date_created TIMESTAMP,
    description VARCHAR(1000),
    name VARCHAR(255),
    admin_id VARCHAR(255),
    FOREIGN KEY (admin_id) REFERENCES users(id)
);

-- Tabla: reading_clubs (clubes de lectura)
CREATE TABLE reading_clubs (
    id VARCHAR(255) PRIMARY KEY,
    date_created TIMESTAMP,
    description VARCHAR(1000),
    last_updated TIMESTAMP,
    name VARCHAR(255),
    book_id BIGINT,
    community_id VARCHAR(255),
    moderator_id VARCHAR(255),
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (community_id) REFERENCES community(id),
    FOREIGN KEY (moderator_id) REFERENCES users(id)
);

-- Tabla: reading_club_members (miembros de clubes de lectura)
CREATE TABLE reading_club_members (
    reading_club_id VARCHAR(255),
    user_id VARCHAR(255),
    PRIMARY KEY (reading_club_id, user_id),
    FOREIGN KEY (reading_club_id) REFERENCES reading_clubs(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Tabla: user_books (libros de usuarios)
CREATE TABLE user_books (
    id VARCHAR(255) PRIMARY KEY,
    is_available_for_exchange BOOLEAN,
    status VARCHAR(255),
    book_id BIGINT,
    user_id VARCHAR(255),
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Tabla: book_transaction (transacciones de libros)
CREATE TABLE book_transaction (
    id VARCHAR(255) PRIMARY KEY,
    date_created TIMESTAMP,
    status VARCHAR(255),
    total_amount INTEGER,
    type VARCHAR(255),
    receiver_reputation_id VARCHAR(255),
    sender_reputation_id VARCHAR(255),
    sender_user_id VARCHAR(255),
    FOREIGN KEY (receiver_reputation_id) REFERENCES users(id),
    FOREIGN KEY (sender_reputation_id) REFERENCES users(id),
    FOREIGN KEY (sender_user_id) REFERENCES users(id)
);

-- Tabla: transaction_receiver_books
CREATE TABLE transaction_receiver_books (
    transaction_id VARCHAR(255),
    book_id BIGINT,
    PRIMARY KEY (transaction_id, book_id),
    FOREIGN KEY (transaction_id) REFERENCES book_transaction(id),
    FOREIGN KEY (book_id) REFERENCES books(id)
);

-- Tabla: transaction_sender_books
CREATE TABLE transaction_sender_books (
    transaction_id VARCHAR(255),
    book_id BIGINT,
    PRIMARY KEY (transaction_id, book_id),
    FOREIGN KEY (transaction_id) REFERENCES book_transaction(id),
    FOREIGN KEY (book_id) REFERENCES books(id)
);

-- Tabla: post (publicaciones)
CREATE TABLE post (
    id VARCHAR(255) PRIMARY KEY,
    body VARCHAR(2000),
    date_created TIMESTAMP,
    image VARCHAR(255),
    user_id VARCHAR(255),
    community_id VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (community_id) REFERENCES community(id)
);

-- Tabla: comment (comentarios)
CREATE TABLE comment (
    id VARCHAR(255) PRIMARY KEY,
    body VARCHAR(1000),
    date_created TIMESTAMP,
    user_id VARCHAR(255),
    post_id VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (post_id) REFERENCES post(id)
);

-- Tabla: community_likes (likes de comunidades)
CREATE TABLE community_likes (
    community_id VARCHAR(255),
    user_id VARCHAR(255),
    PRIMARY KEY (community_id, user_id),
    FOREIGN KEY (community_id) REFERENCES community(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Tabla: community_members (miembros de comunidades)
CREATE TABLE community_members (
    community_id VARCHAR(255),
    user_id VARCHAR(255),
    PRIMARY KEY (community_id, user_id),
    FOREIGN KEY (community_id) REFERENCES community(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- =====================================================
-- ÍNDICES PARA OPTIMIZACIÓN
-- =====================================================
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_address_id ON users(address_id);
CREATE INDEX idx_user_follows_follower ON user_follows(follower_id);
CREATE INDEX idx_user_follows_followed ON user_follows(followed_id);
CREATE INDEX idx_books_isbn ON books(isbn);
CREATE INDEX idx_books_title ON books(title);
CREATE INDEX idx_books_author ON books(author);
CREATE INDEX idx_book_categories_book_id ON book_categories(book_id);
CREATE INDEX idx_user_rates_rated_user ON user_rates(rated_user_id);
CREATE INDEX idx_user_rates_rater_user ON user_rates(rater_user_id);
CREATE INDEX idx_post_user_id ON post(user_id);
CREATE INDEX idx_post_community_id ON post(community_id);
CREATE INDEX idx_comment_post_id ON comment(post_id);
CREATE INDEX idx_comment_user_id ON comment(user_id);
CREATE INDEX idx_reading_clubs_community_id ON reading_clubs(community_id);
CREATE INDEX idx_reading_clubs_book_id ON reading_clubs(book_id);
CREATE INDEX idx_user_books_user_id ON user_books(user_id);
CREATE INDEX idx_user_books_book_id ON user_books(book_id);

-- =====================================================
-- COMENTARIOS SOBRE LAS CORRECCIONES PRINCIPALES
-- =====================================================
/*
PRINCIPALES CAMBIOS REALIZADOS:

1. TABLA USERS:
   - Agregados constraints NOT NULL y UNIQUE según UserEntity
   - Campo 'coins' como INTEGER
   - Relación con addresses mediante address_id
   - Campo date_created usando TIMESTAMP

2. TABLA ADDRESSES:
   - Estructura simplificada según AddressEntity del código
   - Campos: id, state, country, longitude, latitude

3. TABLA USER_FOLLOWS:
   - Una sola tabla para el sistema de seguimiento
   - Reemplaza user_followers y user_following del diagrama
   - Coincide con las queries del UserRepository

4. TABLA BOOKS:
   - ID como BIGSERIAL (auto-incremental)
   - Estructura según BookEntity del código
   - Campo 'author' como VARCHAR (no FK a tabla author)

5. TABLA BOOK_CATEGORIES:
   - Relación Many-to-Many entre books y categories
   - Según la anotación @ElementCollection del código

Las demás tablas mantienen la estructura del diagrama ERD 
para futuras funcionalidades del sistema.
*/ 