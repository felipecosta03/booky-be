-- Eliminar tablas en orden correcto por dependencias
DROP TABLE IF EXISTS user_rates CASCADE;
DROP TABLE IF EXISTS user_achievements CASCADE;
DROP TABLE IF EXISTS achievements CASCADE;
DROP TABLE IF EXISTS gamification_profiles CASCADE;
DROP TABLE IF EXISTS exchange_requester_books CASCADE;
DROP TABLE IF EXISTS exchange_owner_books CASCADE;
DROP TABLE IF EXISTS book_exchanges CASCADE;
DROP TABLE IF EXISTS messages CASCADE;
DROP TABLE IF EXISTS chats CASCADE;
DROP TABLE IF EXISTS comment CASCADE;
DROP TABLE IF EXISTS post CASCADE;
DROP TABLE IF EXISTS reading_club_members CASCADE;
DROP TABLE IF EXISTS reading_clubs CASCADE;
DROP TABLE IF EXISTS community_members CASCADE;
DROP TABLE IF EXISTS community CASCADE;
DROP TABLE IF EXISTS user_books CASCADE;
DROP TABLE IF EXISTS book_categories CASCADE;
DROP TABLE IF EXISTS books CASCADE;
DROP TABLE IF EXISTS user_follows CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;
DROP TABLE IF EXISTS user_levels CASCADE;

-- Eliminar tablas no implementadas (por si existen)
DROP TABLE IF EXISTS user_activity_badges CASCADE;
DROP TABLE IF EXISTS user_activity CASCADE;
DROP TABLE IF EXISTS badge CASCADE;
DROP TABLE IF EXISTS level CASCADE;
DROP TABLE IF EXISTS author CASCADE;
DROP TABLE IF EXISTS transaction_receiver_books CASCADE;
DROP TABLE IF EXISTS transaction_sender_books CASCADE;
DROP TABLE IF EXISTS book_transaction CASCADE;
DROP TABLE IF EXISTS community_likes CASCADE;
-- Tabla: addresses
CREATE TABLE addresses
(
    id        VARCHAR(255) PRIMARY KEY,
    state     VARCHAR(255),
    country   VARCHAR(255),
    longitude DOUBLE PRECISION,
    latitude  DOUBLE PRECISION
);

-- Tabla: users 
CREATE TABLE users
(
    id           VARCHAR(255) PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    username     VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    lastname     VARCHAR(255) NOT NULL,
    description  VARCHAR(1000),
    image        VARCHAR(255),
    coins        INTEGER,
    address_id   VARCHAR(255),
    date_created TIMESTAMP,
    FOREIGN KEY (address_id) REFERENCES addresses (id)
);

-- Tabla: user_follows (sistema de seguimiento)
CREATE TABLE user_follows
(
    follower_id VARCHAR(255),
    followed_id VARCHAR(255),
    PRIMARY KEY (follower_id, followed_id),
    FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (followed_id) REFERENCES users (id) ON DELETE CASCADE
);

-- =====================================================
-- TABLAS DE LIBROS
-- =====================================================

-- Tabla: books
CREATE TABLE books
(
    id        VARCHAR(255) PRIMARY KEY,
    isbn      VARCHAR(255) UNIQUE,
    title     VARCHAR(1000),
    overview  TEXT,
    synopsis  TEXT,
    pages     INTEGER,
    edition   VARCHAR(255),
    publisher VARCHAR(500),
    author    VARCHAR(500), -- Campo directo, no FK
    image     TEXT,
    rate      INTEGER
);

-- Tabla: book_categories (relación libros-categorías)
CREATE TABLE book_categories
(
    book_id  VARCHAR(255),
    category VARCHAR(255),
    PRIMARY KEY (book_id, category),
    FOREIGN KEY (book_id) REFERENCES books (id) ON DELETE CASCADE
);

-- Tabla: user_books (libros de usuarios)
CREATE TABLE user_books
(
    id                VARCHAR(255) PRIMARY KEY,
    user_id           VARCHAR(255) NOT NULL,
    book_id           VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NOT NULL,
    is_favorite       BOOLEAN      NOT NULL DEFAULT FALSE,
    wants_to_exchange BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (book_id) REFERENCES books (id),
    FOREIGN KEY (user_id) REFERENCES users (id),
    UNIQUE (user_id, book_id)
);

-- =====================================================
-- TABLAS DE INTERCAMBIOS
-- =====================================================

-- Tabla: book_exchanges
CREATE TABLE book_exchanges
(
    id           VARCHAR(255) PRIMARY KEY,
    requester_id VARCHAR(255) NOT NULL,
    owner_id     VARCHAR(255) NOT NULL,
    status       VARCHAR(255) NOT NULL,
    date_created TIMESTAMP    NOT NULL,
    date_updated TIMESTAMP    NOT NULL,
    chat_id      VARCHAR(255),
    FOREIGN KEY (requester_id) REFERENCES users (id),
    FOREIGN KEY (owner_id) REFERENCES users (id)
);

-- Tabla: exchange_owner_books
CREATE TABLE exchange_owner_books
(
    exchange_id  VARCHAR(255),
    user_book_id VARCHAR(255),
    PRIMARY KEY (exchange_id, user_book_id),
    FOREIGN KEY (exchange_id) REFERENCES book_exchanges (id) ON DELETE CASCADE
);

-- Tabla: exchange_requester_books  
CREATE TABLE exchange_requester_books
(
    exchange_id  VARCHAR(255),
    user_book_id VARCHAR(255),
    PRIMARY KEY (exchange_id, user_book_id),
    FOREIGN KEY (exchange_id) REFERENCES book_exchanges (id) ON DELETE CASCADE
);

-- Tabla: user_rates (calificaciones de intercambios)
CREATE TABLE user_rates
(
    id           VARCHAR(255) PRIMARY KEY,
    user_id      VARCHAR(255) NOT NULL,
    exchange_id  VARCHAR(255) NOT NULL,
    rating       INTEGER      NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment      VARCHAR(500),
    date_created TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (exchange_id) REFERENCES book_exchanges (id) ON DELETE CASCADE,
    CONSTRAINT uk_user_exchange_rating UNIQUE (user_id, exchange_id)
);

-- =====================================================
-- TABLAS DE CHAT
-- =====================================================

-- Tabla: chats
CREATE TABLE chats
(
    id           VARCHAR(255) PRIMARY KEY,
    user1_id     VARCHAR(255) NOT NULL,
    user2_id     VARCHAR(255) NOT NULL,
    date_created TIMESTAMP    NOT NULL,
    date_updated TIMESTAMP    NOT NULL,
    FOREIGN KEY (user1_id) REFERENCES users (id),
    FOREIGN KEY (user2_id) REFERENCES users (id),
    CONSTRAINT unique_chat_users UNIQUE (user1_id, user2_id)
);

-- Tabla: messages
CREATE TABLE messages
(
    id        VARCHAR(255) PRIMARY KEY,
    chat_id   VARCHAR(255) NOT NULL,
    sender_id VARCHAR(255) NOT NULL,
    content   TEXT         NOT NULL,
    date_sent TIMESTAMP    NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
    FOREIGN KEY (sender_id) REFERENCES users (id)
);

-- Agregar foreign key para chat_id en book_exchanges
ALTER TABLE book_exchanges ADD FOREIGN KEY (chat_id) REFERENCES chats (id);

-- =====================================================
-- TABLAS DE COMUNIDADES
-- =====================================================

-- Tabla: community
CREATE TABLE community
(
    id           VARCHAR(255) PRIMARY KEY,
    date_created TIMESTAMP,
    description  VARCHAR(1000),
    name         VARCHAR(255),
    admin_id     VARCHAR(255),
    FOREIGN KEY (admin_id) REFERENCES users (id)
);

-- Tabla: community_members
CREATE TABLE community_members
(
    community_id VARCHAR(255),
    user_id      VARCHAR(255),
    PRIMARY KEY (community_id, user_id),
    FOREIGN KEY (community_id) REFERENCES community (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Tabla: reading_clubs
CREATE TABLE reading_clubs
(
    id           VARCHAR(255) PRIMARY KEY,
    date_created TIMESTAMP,
    description  VARCHAR(1000),
    last_updated TIMESTAMP,
    name         VARCHAR(255),
    book_id      VARCHAR(255),
    community_id VARCHAR(255),
    moderator_id VARCHAR(255),
    FOREIGN KEY (book_id) REFERENCES books (id),
    FOREIGN KEY (community_id) REFERENCES community (id),
    FOREIGN KEY (moderator_id) REFERENCES users (id)
);

-- Tabla: reading_club_members
CREATE TABLE reading_club_members
(
    reading_club_id VARCHAR(255),
    user_id         VARCHAR(255),
    PRIMARY KEY (reading_club_id, user_id),
    FOREIGN KEY (reading_club_id) REFERENCES reading_clubs (id),
    FOREIGN KEY (user_id) REFERENCES users (id)
);

-- =====================================================
-- TABLAS DE POSTS Y COMENTARIOS
-- =====================================================

-- Tabla: post
CREATE TABLE post
(
    id           VARCHAR(255) PRIMARY KEY,
    body         VARCHAR(2000),
    date_created TIMESTAMP,
    image        VARCHAR(255),
    user_id      VARCHAR(255),
    community_id VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (community_id) REFERENCES community (id)
);

-- Tabla: comment
CREATE TABLE comment
(
    id           VARCHAR(255) PRIMARY KEY,
    body         VARCHAR(1000),
    date_created TIMESTAMP,
    user_id      VARCHAR(255),
    post_id      VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users (id),
    FOREIGN KEY (post_id) REFERENCES post (id)
);

-- =====================================================
-- TABLAS DE GAMIFICACIÓN
-- =====================================================

-- Tabla: user_levels

CREATE TABLE user_levels
(
    level       INTEGER PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    min_points  INTEGER      NOT NULL,
    max_points  INTEGER      NOT NULL,
    badge       VARCHAR(100),
    color       VARCHAR(7)
);

CREATE TABLE achievements
(
    id             VARCHAR(50) PRIMARY KEY,
    name           VARCHAR(200) NOT NULL,
    description    TEXT,
    category       VARCHAR(50)  NOT NULL,
    icon           VARCHAR(100),
    required_value INTEGER      NOT NULL,
    condition_type VARCHAR(50)  NOT NULL,
    points_reward  INTEGER      NOT NULL,
    is_active      BOOLEAN      NOT NULL DEFAULT true
);

CREATE TABLE gamification_profiles
(
    id                    VARCHAR(50) PRIMARY KEY,
    user_id               VARCHAR(50) NOT NULL UNIQUE,
    total_points          INTEGER     NOT NULL DEFAULT 0,
    current_level         INTEGER     NOT NULL DEFAULT 1,
    books_read            INTEGER     NOT NULL DEFAULT 0,
    exchanges_completed   INTEGER     NOT NULL DEFAULT 0,
    posts_created         INTEGER     NOT NULL DEFAULT 0,
    comments_created      INTEGER     NOT NULL DEFAULT 0,
    communities_joined    INTEGER     NOT NULL DEFAULT 0,
    communities_created   INTEGER     NOT NULL DEFAULT 0,
    reading_clubs_joined  INTEGER     NOT NULL DEFAULT 0,
    reading_clubs_created INTEGER     NOT NULL DEFAULT 0,
    last_activity         TIMESTAMP,
    date_created          TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (current_level) REFERENCES user_levels (level)
);

CREATE TABLE user_achievements
(
    id             VARCHAR(50) PRIMARY KEY,
    user_id        VARCHAR(50) NOT NULL,
    achievement_id VARCHAR(50) NOT NULL,
    date_earned    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified       BOOLEAN     NOT NULL DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements (id) ON DELETE CASCADE,
    UNIQUE (user_id, achievement_id)
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_username ON users (username);
CREATE INDEX idx_user_follows_follower ON user_follows (follower_id);
CREATE INDEX idx_user_follows_followed ON user_follows (followed_id);
CREATE INDEX idx_books_isbn ON books (isbn);
CREATE INDEX idx_books_title ON books (title);
CREATE INDEX idx_books_author ON books (author);
CREATE INDEX idx_book_categories_book_id ON book_categories (book_id);
CREATE INDEX idx_user_books_user_id ON user_books (user_id);
CREATE INDEX idx_user_books_book_id ON user_books (book_id);
CREATE INDEX idx_user_books_wants_exchange ON user_books (wants_to_exchange);
CREATE INDEX idx_user_books_status ON user_books (status);
CREATE INDEX idx_user_books_favorite ON user_books (is_favorite);
CREATE INDEX idx_book_exchanges_requester ON book_exchanges (requester_id);
CREATE INDEX idx_book_exchanges_owner ON book_exchanges (owner_id);
CREATE INDEX idx_book_exchanges_status ON book_exchanges (status);
CREATE INDEX idx_book_exchanges_date_created ON book_exchanges (date_created);
CREATE INDEX idx_user_rates_user_id ON user_rates (user_id);
CREATE INDEX idx_user_rates_exchange_id ON user_rates (exchange_id);
CREATE INDEX idx_post_user_id ON post (user_id);
CREATE INDEX idx_post_community_id ON post (community_id);
CREATE INDEX idx_comment_post_id ON comment (post_id);
CREATE INDEX idx_comment_user_id ON comment (user_id);
CREATE INDEX idx_reading_clubs_community_id ON reading_clubs (community_id);
CREATE INDEX idx_reading_clubs_book_id ON reading_clubs (book_id);
CREATE INDEX idx_gamification_profiles_user_id ON gamification_profiles (user_id);
CREATE INDEX idx_user_achievements_user_id ON user_achievements (user_id);
CREATE INDEX idx_user_levels_level_number ON user_levels (level);
CREATE INDEX idx_chats_user1 ON chats (user1_id);
CREATE INDEX idx_chats_user2 ON chats (user2_id);
CREATE INDEX idx_chats_updated ON chats (date_updated);
CREATE INDEX idx_messages_chat ON messages (chat_id);
CREATE INDEX idx_messages_sender ON messages (sender_id);
CREATE INDEX idx_messages_date ON messages (date_sent);

-- Insertar niveles de usuario

INSERT INTO user_levels (level, name, description, min_points, max_points, badge, color)
VALUES (1, 'Novato', 'Recién comenzando tu aventura literaria', 0, 99, '🌱', '#28a745'),
       (2, 'Aprendiz', 'Empezando a descubrir el mundo de los libros', 100, 249, '📚', '#17a2b8'),
       (3, 'Lector', 'Ya tienes experiencia con los libros', 250, 499, '🤓', '#6f42c1'),
       (4, 'Bibliófilo', 'Amante verdadero de la literatura', 500, 999, '📖', '#fd7e14'),
       (5, 'Experto', 'Conocedor profundo del mundo literario', 1000, 1999, '🎓', '#e83e8c'),
       (6, 'Maestro', 'Referente en la comunidad de lectores', 2000, 3999, '👑', '#dc3545'),
       (7, 'Leyenda', 'El más alto honor en Booky', 4000, 999999, '⭐', '#ffc107');

INSERT INTO achievements (id, name, description, category, icon, required_value, condition_type, points_reward,
                          is_active)
VALUES ('ach-first-book', 'Primer Libro', 'Agrega tu primer libro a la biblioteca', 'LECTOR', '📚', 1, 'BOOKS_READ', 25,
        true),
       ('ach-reader-novice', 'Lector Novato', 'Lee 5 libros', 'LECTOR', '📖', 5, 'BOOKS_READ', 50, true),
       ('ach-first-exchange', 'Primer Intercambio', 'Completa tu primer intercambio', 'INTERCAMBIO', '🤝', 1,
        'EXCHANGES_COMPLETED', 75, true),
       ('ach-first-post', 'Primera Palabra', 'Escribe tu primer post', 'SOCIAL', '💬', 1, 'POSTS_CREATED', 25, true),
       ('ach-leader', 'Líder', 'Crea una comunidad', 'SOCIAL', '👑', 1, 'COMMUNITIES_CREATED', 200, true),
       ('ach-club-member', 'Nuevo Miembro', 'Únete a tu primer club de lectura', 'CLUB', '📖', 1, 'READING_CLUBS_JOINED',
        50, true),
       ('ach-hundred-points', 'Centurión', 'Alcanza 100 puntos', 'HITO', '💯', 100, 'TOTAL_POINTS', 25, true);

INSERT INTO addresses (id, state, country, longitude, latitude)
VALUES ('addr-100', 'Buenos Aires', 'Argentina', -58.3816, -34.6037),
       ('addr-101', 'Córdoba', 'Argentina', -64.1888, -31.4201),
       ('addr-102', 'Rosario', 'Argentina', -60.6393, -32.9468),
       ('addr-103', 'Mendoza', 'Argentina', -68.8272, -32.8908),
       ('addr-104', 'Mar del Plata', 'Argentina', -57.5759, -38.0055),
       ('addr-105', 'Salta', 'Argentina', -65.4232, -24.7821),
       ('addr-106', 'Tucumán', 'Argentina', -65.2226, -26.8241),
       ('addr-107', 'Santa Fe', 'Argentina', -60.7021, -31.6107),
       ('addr-108', 'Neuquén', 'Argentina', -68.0591, -38.9516),
       ('addr-109', 'Bariloche', 'Argentina', -71.3103, -41.1335),
       ('addr-110', 'Madrid', 'España', -3.7038, 40.4168),
       ('addr-111', 'Barcelona', 'España', 2.1734, 41.3851),
       ('addr-112', 'México DF', 'México', -99.1332, 19.4326),
       ('addr-113', 'Lima', 'Perú', -77.0428, -12.0464),
       ('addr-114', 'Santiago', 'Chile', -70.6693, -33.4489),
       ('addr-115', 'Bogotá', 'Colombia', -74.0721, 4.7110);

\echo '✅ Direcciones insertadas'

-- =====================================================
-- PASO 6: INSERTAR USUARIOS
-- =====================================================
\echo ''
\echo '👥 PASO 6: Insertando usuarios...'

INSERT INTO users (id, email, username, password, name, lastname, description, image, coins, address_id, date_created)
VALUES

-- Usuarios Administradores
('admin-001', 'admin@booky.com', 'admin', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Administrador', 'Sistema', 'Cuenta de administrador del sistema', NULL, 1000, 'addr-100', NOW()),
('admin-002', 'superadmin@booky.com', 'superadmin', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Super', 'Administrador', 'Cuenta de super administrador', NULL, 2000, 'addr-100', NOW()),

-- Usuarios de Prueba - Argentina
('user-001', 'juan.perez@gmail.com', 'juanp', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Juan',
 'Pérez', 'Amante de la literatura clásica y contemporánea', NULL, 150, 'addr-100', NOW()),
('user-002', 'maria.garcia@outlook.com', 'mariag', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'María', 'García', 'Lectora empedernida de novelas de misterio', NULL, 200, 'addr-101', NOW()),
('user-003', 'carlos.rodriguez@yahoo.com', 'carlosr', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Carlos', 'Rodríguez', 'Fanático de la ciencia ficción y fantasía', NULL, 175, 'addr-102', NOW()),
('user-004', 'ana.lopez@gmail.com', 'anal', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Ana',
 'López', 'Especialista en literatura juvenil', NULL, 125, 'addr-103', NOW()),
('user-005', 'luis.martinez@hotmail.com', 'luism', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Luis', 'Martínez', 'Coleccionista de libros de historia', NULL, 300, 'addr-104', NOW()),
('user-006', 'sofia.gonzalez@gmail.com', 'sofiag', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Sofía', 'González', 'Aficionada a la poesía y narrativa', NULL, 180, 'addr-105', NOW()),
('user-007', 'diego.fernandez@yahoo.com', 'diegof', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Diego', 'Fernández', 'Lector de biografías y ensayos', NULL, 220, 'addr-106', NOW()),
('user-008', 'lucia.morales@outlook.com', 'luciam', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Lucía', 'Morales', 'Estudiante de literatura', NULL, 90, 'addr-107', NOW()),
('user-009', 'alejandro.silva@gmail.com', 'alejandros', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Alejandro', 'Silva', 'Escritor y crítico literario', NULL, 250, 'addr-108', NOW()),
('user-010', 'valentina.castro@hotmail.com', 'valentinac',
 '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Valentina', 'Castro', 'Bloguera de reseñas de libros',
 NULL, 160, 'addr-109', NOW()),

-- Usuarios Internacionales
('user-011', 'isabella.martinez@gmail.es', 'isabellam', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Isabella', 'Martínez', 'Profesora de literatura española', NULL, 280, 'addr-110', NOW()),
('user-012', 'pablo.ruiz@outlook.es', 'pablor', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Pablo',
 'Ruiz', 'Traductor literario', NULL, 320, 'addr-111', NOW()),
('user-013', 'fernanda.lopez@yahoo.mx', 'fernandal', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Fernanda', 'López', 'Editora de casa editorial', NULL, 400, 'addr-112', NOW()),
('user-014', 'ricardo.vargas@gmail.pe', 'ricardov', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Ricardo', 'Vargas', 'Bibliotecario y archivista', NULL, 190, 'addr-113', NOW()),
('user-015', 'camila.torres@outlook.cl', 'camilat', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Camila', 'Torres', 'Investigadora literaria', NULL, 230, 'addr-114', NOW()),
('user-016', 'andres.herrera@gmail.co', 'andresh', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy',
 'Andrés', 'Herrera', 'Crítico de literatura latinoamericana', NULL, 270, 'addr-115', NOW());

INSERT INTO gamification_profiles (id, user_id, total_points, current_level, books_read, exchanges_completed,
                                   posts_created, comments_created, communities_joined, communities_created,
                                   reading_clubs_joined, reading_clubs_created, date_created)
SELECT 'gp-' || u.id,
       u.id,
       0,
       1,
       0,
       0,
       0,
       0,
       0,
       0,
       0,
       0,
       NOW()
FROM users u
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO books (id, isbn, title, overview, synopsis, pages, edition, publisher, author, image, rate)
VALUES

-- Clásicos de Literatura
('book-001', '9780141439518', 'Orgullo y Prejuicio',
 'Una novela romántica que explora temas de clase, matrimonio y moralidad',
 'La historia de Elizabeth Bennet y Mr. Darcy, una pareja que debe superar sus diferencias de clase y sus primeras impresiones erróneas para encontrar el amor verdadero.',
 432, '1ra Edición', 'Penguin Classics', 'Jane Austen',
 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400', 9),

('book-002', '9780446310789', 'Matar un Ruiseñor',
 'Una poderosa historia sobre la injusticia racial en el sur de Estados Unidos',
 'Ambientada en los años 30, narra la historia de Scout Finch y su padre Atticus, un abogado que defiende a un hombre negro acusado injustamente de violación.',
 376, '1ra Edición', 'Grand Central Publishing', 'Harper Lee',
 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400', 9),

('book-003', '9780743273565', 'El Gran Gatsby', 'Una crítica al sueño americano a través de los ojos de Jay Gatsby',
 'La historia de Jay Gatsby y su obsesión por Daisy Buchanan, ambientada en la era del jazz de los años 20.', 180,
 '1ra Edición', 'Scribner', 'F. Scott Fitzgerald', 'https://images.unsplash.com/photo-1506905925346-21bda4d32df4?w=400',
 8),

('book-004', '9780451524935', '1984', 'Una distopía sobre totalitarismo y vigilancia',
 'Winston Smith vive en un mundo donde el Gran Hermano controla cada aspecto de la vida, hasta que conoce a Julia y comienza a cuestionar el sistema.',
 328, '1ra Edición', 'Signet Classics', 'George Orwell',
 'https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=400', 9),

('book-005', '9780553380163', 'Un Mundo Feliz', 'Una sociedad utópica que esconde un oscuro control social',
 'En el año 2540, la humanidad vive en una sociedad aparentemente perfecta donde no existe el dolor, pero tampoco la libertad individual.',
 268, '1ra Edición', 'Bantam Classics', 'Aldous Huxley',
 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400', 8),

-- Literatura Contemporánea
('book-006', '9780062315007', 'El Alquimista', 'Una fábula sobre seguir los sueños personales',
 'Santiago, un pastor andaluz, viaja desde España hasta las pirámides de Egipto en busca de un tesoro, pero encuentra algo mucho más valioso.',
 163, '1ra Edición', 'HarperOne', 'Paulo Coelho', 'https://images.unsplash.com/photo-1532012197267-da84d127e765?w=400',
 7),

('book-007', '9780375760891', 'La Ladrona de Libros', 'Una historia conmovedora ambientada en la Alemania nazi',
 'Liesel, una niña alemana, roba libros y los comparte con otros, incluyendo a un judío escondido en su sótano.', 552,
 '1ra Edición', 'Knopf Books', 'Markus Zusak', 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400', 9),

('book-008', '9780385537859', 'Bajo la Misma Estrella', 'Una historia de amor entre dos adolescentes con cáncer',
 'Hazel y Augustus se conocen en un grupo de apoyo para jóvenes con cáncer y viven una intensa historia de amor.', 313,
 '1ra Edición', 'Dutton Books', 'John Green', 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400', 8),

-- Fantasía y Ciencia Ficción
('book-009', '9780439708180', 'Harry Potter y la Piedra Filosofal', 'El inicio de la saga mágica más famosa',
 'Harry Potter descubre en su undécimo cumpleaños que es un mago y debe enfrentarse al mago oscuro que mató a sus padres.',
 309, '1ra Edición', 'Scholastic', 'J.K. Rowling', 'https://images.unsplash.com/photo-1621351183012-e2f9972dd9bf?w=400',
 9),

('book-010', '9780307887436', 'La Chica del Dragón Tatuado', 'Un thriller sueco lleno de misterio',
 'Un periodista y una hacker antisocial investigan la desaparición de una mujer de una poderosa familia sueca.', 465,
 '1ra Edición', 'Vintage Crime', 'Stieg Larsson', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400',
 8),

-- Más libros para mayor variedad
('book-011', '9780316769174', 'El Guardián entre el Centeno', 'Una novela de iniciación adolescente',
 'Holden Caulfield narra sus experiencias después de ser expulsado de una escuela preparatoria.', 277, '1ra Edición',
 'Little, Brown', 'J.D. Salinger', 'https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400', 7),

('book-012', '9780452284234', 'Rebelión en la Granja', 'Una alegoría sobre el totalitarismo',
 'Los animales de una granja se rebelan contra sus dueños humanos, esperando crear una sociedad donde todos los animales sean iguales.',
 112, '1ra Edición', 'Plume Books', 'George Orwell',
 'https://images.unsplash.com/photo-1495640388908-05fa85288e61?w=400', 8),

('book-013', '9780486282114', 'El Retrato de Dorian Gray',
 'Una historia sobre la belleza, la moralidad y la corrupción',
 'Dorian Gray mantiene su juventud mientras su retrato envejece y se vuelve grotesco, reflejando su alma corrupta.',
 254, '1ra Edición', 'Dover Publications', 'Oscar Wilde',
 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=400', 8),

('book-014', '9780140449136', 'Crimen y Castigo', 'Una profunda exploración psicológica del crimen',
 'Raskolnikov, un estudiante empobrecido, comete un asesinato y lucha con las consecuencias psicológicas de su acto.',
 671, '1ra Edición', 'Penguin Classics', 'Fyodor Dostoevsky',
 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400', 9),

('book-015', '9780060935467', 'Cien Años de Soledad', 'Una obra maestra del realismo mágico',
 'La historia de la familia Buendía a lo largo de siete generaciones en el pueblo ficticio de Macondo.', 417,
 '1ra Edición', 'Harper Perennial', 'Gabriel García Márquez',
 'https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=400', 9);

INSERT INTO book_categories (book_id, category)
VALUES
-- Orgullo y Prejuicio
('book-001', 'Romance'),
('book-001', 'Literatura Clásica'),
('book-001', 'Ficción Histórica'),

-- Matar un Ruiseñor
('book-002', 'Drama'),
('book-002', 'Literatura Clásica'),
('book-002', 'Ficción Social'),

-- El Gran Gatsby
('book-003', 'Drama'),
('book-003', 'Literatura Clásica'),
('book-003', 'Ficción Americana'),

-- 1984
('book-004', 'Ciencia Ficción'),
('book-004', 'Distopía'),
('book-004', 'Literatura Clásica'),

-- Un Mundo Feliz
('book-005', 'Ciencia Ficción'),
('book-005', 'Distopía'),
('book-005', 'Filosofía'),

-- El Alquimista
('book-006', 'Autoayuda'),
('book-006', 'Filosofía'),
('book-006', 'Aventura'),

-- La Ladrona de Libros
('book-007', 'Drama'),
('book-007', 'Ficción Histórica'),
('book-007', 'Juvenil'),

-- Bajo la Misma Estrella
('book-008', 'Romance'),
('book-008', 'Drama'),
('book-008', 'Juvenil'),

-- Harry Potter
('book-009', 'Fantasía'),
('book-009', 'Juvenil'),
('book-009', 'Aventura'),

-- La Chica del Dragón Tatuado
('book-010', 'Thriller'),
('book-010', 'Misterio'),
('book-010', 'Crimen'),

-- Más categorías
('book-011', 'Literatura Clásica'),
('book-011', 'Juvenil'),
('book-012', 'Alegoría'),
('book-012', 'Literatura Clásica'),
('book-012', 'Política'),
('book-013', 'Terror'),
('book-013', 'Literatura Clásica'),
('book-013', 'Filosofía'),
('book-014', 'Drama'),
('book-014', 'Literatura Clásica'),
('book-014', 'Psicología'),
('book-015', 'Realismo Mágico'),
('book-015', 'Literatura Clásica'),
('book-015', 'Familia');

INSERT INTO user_books (id, user_id, book_id, status, is_favorite, wants_to_exchange)
VALUES

-- Usuario 1 (Juan Pérez) - Literatura clásica
('ub-001', 'user-001', 'book-001', 'READ', true, false),      -- Orgullo y Prejuicio (favorito)
('ub-002', 'user-001', 'book-003', 'READ', false, true),      -- El Gran Gatsby (para intercambio)
('ub-003', 'user-001', 'book-004', 'READING', false, false),  -- 1984 (leyendo)
('ub-004', 'user-001', 'book-014', 'TO_READ', false, false),  -- Crimen y Castigo (por leer)
('ub-005', 'user-001', 'book-015', 'READ', true, true),       -- Cien Años de Soledad (favorito + intercambio)

-- Usuario 2 (María García) - Misterio y thriller
('ub-006', 'user-002', 'book-010', 'READ', true, false),      -- La Chica del Dragón Tatuado (favorito)
('ub-007', 'user-002', 'book-002', 'READ', false, true),      -- Matar un Ruiseñor (para intercambio)
('ub-008', 'user-002', 'book-007', 'READING', false, false),  -- La Ladrona de Libros (leyendo)
('ub-009', 'user-002', 'book-011', 'WISHLIST', false, false), -- El Guardián (wishlist)
('ub-010', 'user-002', 'book-013', 'READ', false, true),      -- Dorian Gray (para intercambio)

-- Usuario 3 (Carlos Rodríguez) - Ciencia ficción y fantasía
('ub-011', 'user-003', 'book-004', 'READ', true, false),      -- 1984 (favorito)
('ub-012', 'user-003', 'book-005', 'READ', true, true),       -- Un Mundo Feliz (favorito + intercambio)
('ub-013', 'user-003', 'book-009', 'READ', false, true),      -- Harry Potter (para intercambio)
('ub-014', 'user-003', 'book-012', 'READING', false, false),  -- Rebelión en la Granja (leyendo)
('ub-015', 'user-003', 'book-001', 'TO_READ', false, false),  -- Orgullo y Prejuicio (por leer)

-- Usuario 4 (Ana López) - Literatura juvenil
('ub-016', 'user-004', 'book-008', 'READ', true, false),      -- Bajo la Misma Estrella (favorito)
('ub-017', 'user-004', 'book-009', 'READ', true, true),       -- Harry Potter (favorito + intercambio)
('ub-018', 'user-004', 'book-007', 'READ', false, true),      -- La Ladrona de Libros (para intercambio)
('ub-019', 'user-004', 'book-011', 'READING', false, false),  -- El Guardián (leyendo)
('ub-020', 'user-004', 'book-006', 'WISHLIST', false, false), -- El Alquimista (wishlist)

-- Usuario 5 (Luis Martínez) - Historia y biografías
('ub-021', 'user-005', 'book-002', 'READ', true, false),      -- Matar un Ruiseñor (favorito)
('ub-022', 'user-005', 'book-014', 'READ', false, true),      -- Crimen y Castigo (para intercambio)
('ub-023', 'user-005', 'book-015', 'READING', false, false),  -- Cien Años de Soledad (leyendo)
('ub-024', 'user-005', 'book-013', 'TO_READ', false, false),  -- Dorian Gray (por leer)
('ub-025', 'user-005', 'book-003', 'READ', false, true),      -- El Gran Gatsby (para intercambio)

-- Usuario 6 (Sofía González) - Poesía y narrativa
('ub-026', 'user-006', 'book-006', 'READ', true, false),      -- El Alquimista (favorito)
('ub-027', 'user-006', 'book-013', 'READ', false, true),      -- Dorian Gray (para intercambio)
('ub-028', 'user-006', 'book-001', 'READING', false, false),  -- Orgullo y Prejuicio (leyendo)
('ub-029', 'user-006', 'book-015', 'TO_READ', false, false),  -- Cien Años de Soledad (por leer)
('ub-030', 'user-006', 'book-008', 'WISHLIST', false, false), -- Bajo la Misma Estrella (wishlist)

-- Usuario 7 (Diego Fernández) - Biografías y ensayos
('ub-031', 'user-007', 'book-012', 'READ', true, false),      -- Rebelión en la Granja (favorito)
('ub-032', 'user-007', 'book-004', 'READ', false, true),      -- 1984 (para intercambio)
('ub-033', 'user-007', 'book-010', 'READING', false, false),  -- La Chica del Dragón (leyendo)
('ub-034', 'user-007', 'book-005', 'WISHLIST', false, false), -- Un Mundo Feliz (wishlist)
('ub-035', 'user-007', 'book-006', 'READ', false, true),      -- El Alquimista (para intercambio)

-- Usuario 8 (Lucía Morales) - Estudiante de literatura
('ub-036', 'user-008', 'book-011', 'READ', true, false),      -- El Guardián (favorito)
('ub-037', 'user-008', 'book-003', 'READ', false, true),      -- El Gran Gatsby (para intercambio)
('ub-038', 'user-008', 'book-001', 'READING', false, false),  -- Orgullo y Prejuicio (leyendo)
('ub-039', 'user-008', 'book-014', 'TO_READ', false, false),  -- Crimen y Castigo (por leer)
('ub-040', 'user-008', 'book-007', 'WISHLIST', false, false); -- La Ladrona de Libros (wishlist)

INSERT INTO community (id, date_created, description, name, admin_id)
VALUES

-- Comunidades Literarias Generales
('comm-001', NOW(),
 'Espacio para amantes de la literatura clásica y contemporánea. Compartimos reseñas, recomendaciones y debates sobre grandes obras.',
 'Literatura Clásica', 'user-001'),
('comm-002', NOW(),
 'Comunidad dedicada a los misterios más intrigantes de la literatura. Desde Agatha Christie hasta autores contemporáneos.',
 'Club de Misterio', 'user-002'),
('comm-003', NOW(),
 'Para fanáticos de mundos fantásticos, naves espaciales y criaturas míticas. Ciencia ficción y fantasía sin límites.',
 'Sci-Fi & Fantasy', 'user-003'),
('comm-004', NOW(),
 'Descubre las mejores historias para jóvenes lectores. Desde clásicos juveniles hasta nuevas tendencias.',
 'Literatura Juvenil', 'user-004'),
('comm-005', NOW(), 'Exploramos el pasado a través de biografías, ensayos históricos y documentos que marcaron épocas.',
 'Historia y Biografías', 'user-005'),

-- Comunidades por Géneros Específicos
('comm-006', NOW(), 'Versos que tocan el alma. Compartimos poesía clásica, contemporánea y creaciones propias.',
 'Poesía Universal', 'user-006'),
('comm-007', NOW(), 'Reflexiones profundas, ensayos filosóficos y textos que nos hacen pensar diferente.',
 'Ensayos y Filosofía', 'user-007'),
('comm-008', NOW(),
 'Para estudiantes y académicos. Analizamos textos, compartimos recursos y discutimos técnicas literarias.',
 'Análisis Literario', 'user-008'),
('comm-009', NOW(), 'Escritores, críticos y editores compartiendo experiencias del mundo editorial y creativo.',
 'Escritores Unidos', 'user-009'),
('comm-010', NOW(), 'Reseñas honestas, recomendaciones personalizadas y debates sobre las últimas publicaciones.',
 'Reseñas y Críticas', 'user-010'),

-- Comunidades Regionales
('comm-011', NOW(), 'Celebramos la rica tradición literaria de España, desde el Siglo de Oro hasta la actualidad.',
 'Literatura Española', 'user-011'),
('comm-012', NOW(), 'Traducción como arte: compartimos técnicas, desafíos y bellezas de llevar textos entre idiomas.',
 'Arte de Traducir', 'user-012'),
('comm-013', NOW(), 'Para profesionales del mundo editorial: editores, correctores, diseñadores y distribuidores.',
 'Industria Editorial', 'user-013'),
('comm-014', NOW(), 'Bibliotecarios, archivistas y custodios del conocimiento compartiendo recursos y experiencias.',
 'Guardianes del Saber', 'user-014'),
('comm-015', NOW(), 'Investigación académica en literatura: metodologías, proyectos y descubrimientos.',
 'Investigación Literaria', 'user-015'),
('comm-016', NOW(), 'La voz única de América Latina: desde Borges hasta los nuevos talentos emergentes.',
 'Literatura Latinoamericana', 'user-016'),

-- Comunidades Temáticas Especiales
('comm-017', NOW(), 'Libros que nos ayudan a crecer, mejorar y encontrar nuestro camino en la vida.',
 'Desarrollo Personal', 'admin-001'),
('comm-018', NOW(), 'Novelas gráficas, cómics y literatura visual. El arte de contar historias con imágenes.',
 'Novela Gráfica', 'admin-002'),
('comm-019', NOW(), 'Intercambio físico de libros entre miembros. Dale una segunda vida a tus lecturas.',
 'Intercambio de Libros', 'user-001'),
('comm-020', NOW(), 'Desafíos de lectura mensuales, maratones literarios y metas compartidas.', 'Desafíos Lectores',
 'user-002');

-- Los administradores son automáticamente miembros de sus comunidades
INSERT INTO community_members (community_id, user_id)
VALUES ('comm-001', 'user-001'),
       ('comm-002', 'user-002'),
       ('comm-003', 'user-003'),
       ('comm-004', 'user-004'),
       ('comm-005', 'user-005'),
       ('comm-006', 'user-006'),
       ('comm-007', 'user-007'),
       ('comm-008', 'user-008'),
       ('comm-009', 'user-009'),
       ('comm-010', 'user-010'),
       ('comm-011', 'user-011'),
       ('comm-012', 'user-012'),
       ('comm-013', 'user-013'),
       ('comm-014', 'user-014'),
       ('comm-015', 'user-015'),
       ('comm-016', 'user-016'),
       ('comm-017', 'admin-001'),
       ('comm-018', 'admin-002'),
       ('comm-019', 'user-001'),
       ('comm-020', 'user-002');

-- Membresías cruzadas (usuarios uniéndose a múltiples comunidades)
INSERT INTO community_members (community_id, user_id)
VALUES
-- Juan (user-001) se une a varias comunidades
('comm-002', 'user-001'),  -- Club de Misterio
('comm-005', 'user-001'),  -- Historia y Biografías
('comm-016', 'user-001'),  -- Literatura Latinoamericana
('comm-020', 'user-001'),  -- Desafíos Lectores

-- María (user-002) diversifica sus intereses
('comm-001', 'user-002'),  -- Literatura Clásica
('comm-003', 'user-002'),  -- Sci-Fi & Fantasy
('comm-010', 'user-002'),  -- Reseñas y Críticas
('comm-019', 'user-002'),  -- Intercambio de Libros

-- Carlos (user-003) explora más géneros
('comm-001', 'user-003'),  -- Literatura Clásica
('comm-006', 'user-003'),  -- Poesía Universal
('comm-009', 'user-003'),  -- Escritores Unidos
('comm-017', 'user-003'),  -- Desarrollo Personal

-- Ana (user-004) se especializa en educación
('comm-008', 'user-004'),  -- Análisis Literario
('comm-013', 'user-004'),  -- Industria Editorial
('comm-014', 'user-004'),  -- Guardianes del Saber
('comm-020', 'user-004'),  -- Desafíos Lectores

-- Luis (user-005) profundiza en historia
('comm-007', 'user-005'),  -- Ensayos y Filosofía
('comm-015', 'user-005'),  -- Investigación Literaria
('comm-016', 'user-005'),  -- Literatura Latinoamericana
('comm-017', 'user-005'),  -- Desarrollo Personal

-- Sofía (user-006) ama la creatividad
('comm-009', 'user-006'),  -- Escritores Unidos
('comm-010', 'user-006'),  -- Reseñas y Críticas
('comm-011', 'user-006'),  -- Literatura Española
('comm-018', 'user-006'),  -- Novela Gráfica

-- Membresías de usuarios internacionales
('comm-001', 'user-011'),  -- Isabella en Literatura Clásica
('comm-006', 'user-011'),  -- Isabella en Poesía Universal
('comm-016', 'user-011'),  -- Isabella en Literatura Latinoamericana

('comm-012', 'user-013'),  -- Fernanda interesada en traducción
('comm-015', 'user-013'),  -- Fernanda en investigación
('comm-016', 'user-013'),  -- Fernanda en Literatura Latinoamericana

-- Administradores también participan en otras comunidades
('comm-003', 'admin-001'), -- Admin en Sci-Fi & Fantasy
('comm-009', 'admin-001'), -- Admin en Escritores Unidos
('comm-001', 'admin-002'), -- Super Admin en Literatura Clásica
('comm-010', 'admin-002'); -- Super Admin en Reseñas y Críticas

INSERT INTO reading_clubs (id, date_created, description, last_updated, name, book_id, community_id, moderator_id)
VALUES

-- Clubes en comunidades existentes
('club-001', NOW() - INTERVAL '1 month',
 'Club dedicado a la lectura y análisis profundo de "Matar un Ruiseñor". Exploramos temas de justicia, racismo y crecimiento moral.',
 NOW() - INTERVAL '1 week', 'Club Mockingbird', 'book-002', 'comm-001', 'user-001'),

('club-002', NOW() - INTERVAL '3 weeks',
 'Redescubriendo la magia de Harry Potter desde una perspectiva adulta. Analizamos simbolismos, referencias y el mundo mágico de Rowling.',
 NOW() - INTERVAL '3 days', 'Magia Adulta', 'book-009', 'comm-004', 'user-004'),

('club-003', NOW() - INTERVAL '2 weeks',
 'Distopía orwelliana en tiempos modernos. ¿Qué tan cerca estamos del mundo de Winston Smith? Debate y reflexión crítica.',
 NOW() - INTERVAL '5 days', 'Hermano Mayor', 'book-004', 'comm-003', 'user-003'),

('club-004', NOW() - INTERVAL '1 week',
 'Huxley vs Orwell: dos visiones del futuro. ¿Control por placer o por miedo? Comparamos ambas distopías.',
 NOW() - INTERVAL '2 days', 'Mundo Feliz', 'book-005', 'comm-007', 'user-007'),

('club-005', NOW() - INTERVAL '5 days',
 'La amistad y los sueños rotos en la América de la Depresión. Steinbeck y su retrato crudo de la humanidad.',
 NOW() - INTERVAL '1 day', 'Ratones y Hombres', 'book-014', 'comm-005', 'user-005');

INSERT INTO reading_club_members (reading_club_id, user_id)
VALUES

-- Moderadores son automáticamente miembros
('club-001', 'user-001'),
('club-002', 'user-004'),
('club-003', 'user-003'),
('club-004', 'user-007'),
('club-005', 'user-005'),

-- Miembros adicionales basados en intereses
-- Club Mockingbird (Literatura Clásica)
('club-001', 'user-002'),  -- María (amante de misterios, puede interesarle análisis social)
('club-001', 'user-008'),  -- Lucía (estudiante de literatura)
('club-001', 'user-011'),  -- Isabella (profesora de literatura)
('club-001', 'admin-002'), -- Super Admin

-- Magia Adulta (Literatura Juvenil)
('club-002', 'user-001'),  -- Juan (literatura clásica, puede disfrutar análisis profundo)
('club-002', 'user-006'),  -- Sofía (poesía, creatividad)
('club-002', 'user-008'),  -- Lucía (estudiante)
('club-002', 'user-010'),  -- Valentina (reseñas)

-- Hermano Mayor (Sci-Fi & Fantasy)
('club-003', 'user-001'),  -- Juan (intereses amplios)
('club-003', 'user-002'),  -- María (también en sci-fi)
('club-003', 'user-007'),  -- Diego (ensayos, filosofía)
('club-003', 'user-009'),  -- Alejandro (escritor, crítico)

-- Mundo Feliz (Ensayos y Filosofía)
('club-004', 'user-003'),  -- Carlos (sci-fi, puede apreciar filosofía)
('club-004', 'user-005'),  -- Luis (historia, ensayos)
('club-004', 'user-015'),  -- Camila (investigación)
('club-004', 'admin-001'), -- Admin

-- Ratones y Hombres (Historia y Biografías)
('club-005', 'user-001'),  -- Juan (literatura clásica)
('club-005', 'user-007'),  -- Diego (ensayos)
('club-005', 'user-014'),  -- Ricardo (bibliotecario)
('club-005', 'user-016'); -- Andrés (literatura latinoamericana)

INSERT INTO user_follows (follower_id, followed_id)
VALUES ('user-001', 'user-002'),
       ('user-001', 'user-003'),
       ('user-001', 'user-009'),
       ('user-002', 'user-001'),
       ('user-002', 'user-004'),
       ('user-002', 'user-010'),
       ('user-003', 'user-001'),
       ('user-003', 'user-002'),
       ('user-003', 'user-006'),
       ('user-004', 'user-008'),
       ('user-005', 'user-007'),
       ('user-006', 'user-009'),
       ('user-009', 'user-006'),
       ('user-010', 'user-002'),
       ('admin-001', 'user-009');

-- POSTS GENERALES
INSERT INTO post (id, body, date_created, user_id, community_id)
VALUES ('post-001',
        '¡Acabo de terminar "Cien años de soledad"! La magia de García Márquez es incomparable. ¿Alguien más se quedó viviendo en Macondo? 📚✨',
        NOW() - INTERVAL '2 hours', 'user-001', NULL),
       ('post-002',
        'Organizando mi biblioteca y tengo más libros sin leer que leídos. ¿Les pasa lo mismo? #ProblemasDeUnaLectora',
        NOW() - INTERVAL '5 hours', 'user-002', NULL),
       ('post-003', 'Debate: ¿Philip K. Dick o Isaac Asimov? Necesito opiniones para introducir a alguien al sci-fi.',
        NOW() - INTERVAL '8 hours', 'user-003', NULL),
       ('post-004',
        'La literatura juvenil contemporánea aborda temas reales: ansiedad, identidad, justicia social. Está más madura que nunca.',
        NOW() - INTERVAL '12 hours', 'user-004', NULL),
       ('post-005',
        'Encontré una biografía de Da Vinci con anotaciones del dueño anterior. Los libros usados tienen historias dentro de historias.',
        NOW() - INTERVAL '1 day', 'user-005', NULL),
       ('post-006',
        'Escribí un poema inspirado en esta lluvia. La poesía está en todos lados, solo hay que estar dispuesto a verla. 🌧️',
        NOW() - INTERVAL '2 days', 'user-006', NULL);

-- POSTS EN COMUNIDADES
INSERT INTO post (id, body, date_created, user_id, community_id)
VALUES ('post-007',
        '¿Es Emma Bovary víctima del romanticismo o adelantada a su tiempo? Flaubert creó un personaje complejo. Abramos el debate.',
        NOW() - INTERVAL '3 hours', 'user-001', 'comm-001'),
       ('post-008',
        '¿Alguien más piensa que Christie escribió el mejor final en "El asesinato de Roger Ackroyd"? Pura genialidad narrativa.',
        NOW() - INTERVAL '6 hours', 'user-002', 'comm-002'),
       ('post-009', '¿"Dune" es la mejor obra de sci-fi? La construcción del mundo de Herbert es increíble.',
        NOW() - INTERVAL '10 hours', 'user-003', 'comm-003'),
       ('post-010',
        'Los libros YA abordan temas profundos. "The Hate U Give" habla de racismo con madurez impresionante.',
        NOW() - INTERVAL '4 hours', 'user-004', 'comm-004'),
       ('post-011', 'INTERCAMBIO: "Los pilares de la Tierra" por algo de Isabel Allende o Murakami. ¡CABA! 📚🔄',
        NOW() - INTERVAL '1 day', 'user-001', 'comm-019');

INSERT INTO comment (id, body, date_created, user_id, post_id)
VALUES ('comm-001', 'Macondo es más real que muchos lugares reales. García Márquez tenía esa magia única.',
        NOW() - INTERVAL '1 hour', 'user-002', 'post-001'),
       ('comm-002', '¡Ese es el síndrome del TBR infinito! Parte de la magia de ser lector.',
        NOW() - INTERVAL '4 hours', 'user-001', 'post-002'),
       ('comm-003', 'Dick para filosofía, Asimov para aventura. Para introducir: "Yo, robot" de Asimov.',
        NOW() - INTERVAL '7 hours', 'user-001', 'post-003'),
       ('comm-004', 'Emma es víctima de su época. Una mujer inteligente atrapada en un mundo limitante.',
        NOW() - INTERVAL '2 hours', 'user-006', 'post-007'),
       ('comm-005', '¡SÍ! Ese final me rompió la cabeza. Christie era maestra del plot twist.',
        NOW() - INTERVAL '5 hours', 'user-001', 'post-008'),
       ('comm-006', '¡Tengo "La casa de los espíritus"! ¿Te interesa?', NOW() - INTERVAL '18 hours', 'user-005',
        'post-011');

-- =====================================================
-- DATOS DE CHATS Y MENSAJES
-- =====================================================

-- Insertar chats
INSERT INTO chats (id, user1_id, user2_id, date_created, date_updated)
VALUES 
-- Chat entre Juan (user-001) y Carlos (user-003) - Para intercambio
('chat-001', 'user-001', 'user-003', NOW() - INTERVAL '3 days', NOW() - INTERVAL '30 minutes'),

-- Chat entre María (user-002) y Ana (user-004) - Conversación general
('chat-002', 'user-002', 'user-004', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 hours'),

-- Chat entre Luis (user-005) y Sofía (user-006) - Intercambio de libros
('chat-003', 'user-005', 'user-006', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 hour'),

-- Chat entre Diego (user-007) y Lucía (user-008) - Discusión literaria
('chat-004', 'user-007', 'user-008', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '15 minutes'),

-- Chat entre Juan (user-001) y María (user-002) - Recomendaciones
('chat-005', 'user-001', 'user-002', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '45 minutes');

-- Insertar mensajes
INSERT INTO messages (id, chat_id, sender_id, content, date_sent, is_read)
VALUES 
-- Conversación en chat-001 (Juan y Carlos - Intercambio)
('msg-001', 'chat-001', 'user-001', 'Hola Carlos! Vi que tienes "Un Mundo Feliz" disponible para intercambio. Me interesa mucho.', NOW() - INTERVAL '3 days', true),
('msg-002', 'chat-001', 'user-003', '¡Hola Juan! Sí, tengo "Un Mundo Feliz" y está en excelente estado. ¿Qué me ofreces a cambio?', NOW() - INTERVAL '3 days' + INTERVAL '15 minutes', true),
('msg-003', 'chat-001', 'user-001', 'Te puedo ofrecer "Cien Años de Soledad". Es una primera edición y está como nuevo.', NOW() - INTERVAL '3 days' + INTERVAL '30 minutes', true),
('msg-004', 'chat-001', 'user-003', 'Perfecto! García Márquez es uno de mis autores favoritos. ¿Dónde podemos hacer el intercambio?', NOW() - INTERVAL '3 days' + INTERVAL '45 minutes', true),
('msg-005', 'chat-001', 'user-001', 'Podemos encontrarnos en el café de Corrientes y Callao el sábado a las 15:00. ¿Te parece bien?', NOW() - INTERVAL '2 days', true),
('msg-006', 'chat-001', 'user-003', 'Perfecto! Nos vemos el sábado. Llevaré "Un Mundo Feliz" en una bolsa azul.', NOW() - INTERVAL '2 days' + INTERVAL '20 minutes', true),
('msg-007', 'chat-001', 'user-001', 'Genial! Yo llevaré "Cien Años de Soledad" en una carpeta marrón. ¡Hasta el sábado!', NOW() - INTERVAL '2 days' + INTERVAL '25 minutes', true),
('msg-008', 'chat-001', 'user-003', '¡Excelente intercambio! El libro está en perfectas condiciones. Gracias Juan!', NOW() - INTERVAL '1 day', true),
('msg-009', 'chat-001', 'user-001', 'Igualmente! "Un Mundo Feliz" es exactamente lo que esperaba. ¡Fue un placer hacer el intercambio contigo!', NOW() - INTERVAL '1 day' + INTERVAL '10 minutes', true),
('msg-010', 'chat-001', 'user-003', 'Si tienes más libros de García Márquez, avísame. Me encanta su estilo.', NOW() - INTERVAL '30 minutes', false),

-- Conversación en chat-002 (María y Ana - Conversación general)
('msg-011', 'chat-002', 'user-002', 'Ana, ¿has leído algo bueno últimamente? Necesito recomendaciones de literatura juvenil.', NOW() - INTERVAL '2 days', true),
('msg-012', 'chat-002', 'user-004', '¡Hola María! Acabo de terminar "La Ladrona de Libros" y me encantó. Es juvenil pero muy profundo.', NOW() - INTERVAL '2 days' + INTERVAL '30 minutes', true),
('msg-013', 'chat-002', 'user-002', 'Ese libro está en mi lista desde hace tiempo. ¿Es muy triste?', NOW() - INTERVAL '2 days' + INTERVAL '45 minutes', true),
('msg-014', 'chat-002', 'user-004', 'Tiene momentos tristes, pero también mucha esperanza. La narrativa es hermosa.', NOW() - INTERVAL '2 days' + INTERVAL '1 hour', true),
('msg-015', 'chat-002', 'user-002', 'Perfecto, creo que será mi próxima lectura. ¿Tienes más recomendaciones?', NOW() - INTERVAL '2 hours', false),

-- Conversación en chat-003 (Luis y Sofía - Intercambio)
('msg-016', 'chat-003', 'user-005', 'Sofía, vi tu post sobre "El Retrato de Dorian Gray". ¿Estarías interesada en intercambiarlo?', NOW() - INTERVAL '1 day', true),
('msg-017', 'chat-003', 'user-006', 'Hola Luis! Sí, me gustaría intercambiarlo. ¿Qué tienes disponible?', NOW() - INTERVAL '1 day' + INTERVAL '20 minutes', true),
('msg-018', 'chat-003', 'user-005', 'Tengo "Crimen y Castigo" de Dostoevsky. Es una edición muy buena.', NOW() - INTERVAL '1 day' + INTERVAL '35 minutes', true),
('msg-019', 'chat-003', 'user-006', '¡Me encanta Dostoevsky! Acepto el intercambio. ¿Cuándo podemos hacerlo?', NOW() - INTERVAL '1 day' + INTERVAL '50 minutes', true),
('msg-020', 'chat-003', 'user-005', 'Podemos hacerlo mañana en la plaza San Martín a las 18:00.', NOW() - INTERVAL '1 hour', false),

-- Conversación en chat-004 (Diego y Lucía - Discusión literaria)
('msg-021', 'chat-004', 'user-007', 'Lucía, leí tu análisis sobre "El Guardián entre el Centeno". Muy interesante tu perspectiva.', NOW() - INTERVAL '5 hours', true),
('msg-022', 'chat-004', 'user-008', 'Gracias Diego! Salinger tiene una forma única de capturar la adolescencia.', NOW() - INTERVAL '5 hours' + INTERVAL '15 minutes', true),
('msg-023', 'chat-004', 'user-007', 'Exacto. ¿Has leído "Franny and Zooey"? Es menos conocido pero igual de profundo.', NOW() - INTERVAL '4 hours', true),
('msg-024', 'chat-004', 'user-008', 'No, pero ahora lo agregaré a mi lista. ¿Me lo recomiendas?', NOW() - INTERVAL '15 minutes', false),

-- Conversación en chat-005 (Juan y María - Recomendaciones)
('msg-025', 'chat-005', 'user-001', 'María, ¿qué opinas de los thrillers nórdicos? Estoy pensando en leer algo del género.', NOW() - INTERVAL '6 hours', true),
('msg-026', 'chat-005', 'user-002', '¡Son fantásticos! Te recomiendo empezar con "La Chica del Dragón Tatuado".', NOW() - INTERVAL '6 hours' + INTERVAL '10 minutes', true),
('msg-027', 'chat-005', 'user-001', 'Perfecto, justo vi que lo tienes en tu biblioteca. ¿Es muy complejo?', NOW() - INTERVAL '5 hours', true),
('msg-028', 'chat-005', 'user-002', 'Al principio puede parecer lento, pero después no podrás dejarlo. La trama es adictiva.', NOW() - INTERVAL '45 minutes', false);

-- =====================================================
-- DATOS DE INTERCAMBIOS
-- =====================================================

-- Insertar intercambios
INSERT INTO book_exchanges (id, requester_id, owner_id, status, date_created, date_updated, chat_id)
VALUES 
-- Intercambio completado entre Juan y Carlos
('exchange-001', 'user-001', 'user-003', 'COMPLETED', NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day', 'chat-001'),

-- Intercambio pendiente entre Luis y Sofía
('exchange-002', 'user-005', 'user-006', 'PENDING', NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 hour', 'chat-003'),

-- Intercambio aceptado entre María y Ana (sin chat específico aún)
('exchange-003', 'user-002', 'user-004', 'ACCEPTED', NOW() - INTERVAL '2 hours', NOW() - INTERVAL '1 hour', NULL);

-- Insertar libros de intercambios
INSERT INTO exchange_owner_books (exchange_id, user_book_id)
VALUES 
-- Carlos ofrece "Un Mundo Feliz" (ub-012)
('exchange-001', 'ub-012'),
-- Sofía ofrece "Dorian Gray" (ub-027)  
('exchange-002', 'ub-027'),
-- Ana ofrece "Harry Potter" (ub-017)
('exchange-003', 'ub-017');

INSERT INTO exchange_requester_books (exchange_id, user_book_id)
VALUES 
-- Juan ofrece "Cien Años de Soledad" (ub-005)
('exchange-001', 'ub-005'),
-- Luis ofrece "Crimen y Castigo" (ub-022)
('exchange-002', 'ub-022'),
-- María ofrece "Matar un Ruiseñor" (ub-007)
('exchange-003', 'ub-007');

-- Insertar calificación para el intercambio completado
INSERT INTO user_rates (id, user_id, exchange_id, rating, comment, date_created)
VALUES 
('rate-001', 'user-001', 'exchange-001', 5, 'Excelente intercambio! El libro estaba en perfectas condiciones y Carlos fue muy puntual.', NOW() - INTERVAL '1 day'),
('rate-002', 'user-003', 'exchange-001', 5, 'Juan es una persona muy confiable. El intercambio fue perfecto y el libro era exactamente como lo describió.', NOW() - INTERVAL '1 day' + INTERVAL '30 minutes');

SELECT 'Users: ' || COUNT(*)
FROM users
UNION ALL
SELECT 'Books: ' || COUNT(*)
FROM books
UNION ALL
SELECT 'User_books: ' || COUNT(*)
FROM user_books
UNION ALL
SELECT 'Communities: ' || COUNT(*)
FROM community
UNION ALL
SELECT 'Reading_clubs: ' || COUNT(*)
FROM reading_clubs
UNION ALL
SELECT 'Posts: ' || COUNT(*)
FROM post
UNION ALL
SELECT 'Comments: ' || COUNT(*)
FROM comment
UNION ALL
SELECT 'Chats: ' || COUNT(*)
FROM chats
UNION ALL
SELECT 'Messages: ' || COUNT(*)
FROM messages
UNION ALL
SELECT 'Book_exchanges: ' || COUNT(*)
FROM book_exchanges
UNION ALL
SELECT 'User_rates: ' || COUNT(*)
FROM user_rates
UNION ALL
SELECT 'User_levels: ' || COUNT(*)
FROM user_levels
UNION ALL
SELECT 'Achievements: ' || COUNT(*)
FROM achievements
UNION ALL
SELECT 'Gamification_profiles: ' || COUNT(*)
FROM gamification_profiles
UNION ALL
SELECT 'Libros para intercambio: ' || COUNT(*)
FROM user_books
WHERE wants_to_exchange = true
UNION ALL
SELECT 'Libros favoritos: ' || COUNT(*)
FROM user_books
WHERE is_favorite = true
UNION ALL
SELECT 'Mensajes no leídos: ' || COUNT(*)
FROM messages
WHERE is_read = false;

SELECT 'User ID: ' || id || ' - Email: ' || email
FROM users
LIMIT 3;

COMMIT;

-- =====================================================
-- COMENTARIOS FINALES
-- =====================================================
/*
ARCHIVO UNIFICADO - RESUMEN COMPLETO:

✅ TABLAS CREADAS (20 tablas activas):
- addresses, users, user_follows
- books, book_categories, user_books  
- book_exchanges, exchange_owner_books, exchange_requester_books, user_rates
- chats, messages (NUEVO SISTEMA DE CHAT)
- community, community_members, reading_clubs, reading_club_members
- post, comment
- gamification_profiles, achievements, user_achievements, user_levels

❌ TABLAS ELIMINADAS (8 tablas no utilizadas):
- user_activity, level, badge, user_activity_badges
- author, book_transaction, transaction_receiver_books, transaction_sender_books

📊 DATOS INSERTADOS:
- 18 usuarios (16 regulares + 2 admins)
- 16 direcciones en varios países
- 15 libros con categorías completas
- 40 user_books con estados variados
- 20 comunidades temáticas
- 5 clubes de lectura activos
- 11 posts y 6 comentarios
- 5 chats entre usuarios (NUEVO)
- 28 mensajes de chat con conversaciones realistas (NUEVO)
- 3 intercambios de libros con diferentes estados (NUEVO)
- 2 calificaciones de intercambios (NUEVO)
- 7 niveles de usuario y 7 logros
- 18 perfiles de gamificación
- Múltiples membresías cruzadas

🚀 BENEFICIOS:
- Sistema de chat completamente funcional
- Intercambios automáticamente vinculados a chats
- Esquema optimizado y mantenible
- Solo tablas realmente implementadas
- Datos de prueba completos y realistas
- Listo para testing de todas las funcionalidades
- Mejor rendimiento de base de datos
- Documentación precisa del sistema actual

🎯 LISTO PARA USAR:
- Sistema de chat entre usuarios
- Intercambios de libros con chat integrado
- Sistema de intercambios con calificaciones
- Gamificación completa
- Comunidades y clubes de lectura
- Posts y comentarios
- Usuarios con libros para intercambiar

💬 FUNCIONALIDADES DE CHAT:
- Chat único entre dos usuarios
- Mensajes con estado de lectura
- Integración automática con intercambios
- Conversaciones realistas de prueba
- Intercambio completado entre user-001 (Juan) y user-003 (Carlos)
*/
