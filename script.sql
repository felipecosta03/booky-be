-- Script de ejemplo para poblar las tablas addresses y users

-- Insertar direcciones
INSERT INTO addresses (id, state, country, longitude, latitude)
VALUES (1, 'Buenos Aires', 'Argentina', -58.3816, -34.6037),
       (2, 'Madrid', 'España', -3.7038, 40.4168);

-- Insertar usuarios (contraseñas encriptadas con BCrypt)
INSERT INTO users (id, email, username, password, name, lastname, description, image, coins, address_id, date_created)
VALUES ('1', 'juan@mail.com', 'juan123', '$2a$10$7QJ8Qw1Qw1Qw1Qw1Qw1QwOeQw1Qw1Qw1Qw1Qw1Qw1Qw1Qw1Qw1Qw', 'Juan', 'Pérez',
        'Amante de los libros', NULL, 100, 1, NOW()),
       ('2', 'ana@mail.com', 'ana456', '$2a$10$8Rk8Rk8Rk8Rk8Rk8Rk8RkOeRk8Rk8Rk8Rk8Rk8Rk8Rk8Rk8Rk8Rk', 'Ana', 'García',
        'Lectora empedernida', NULL, 200, 2, NOW());

-- NOTA: Reemplaza los valores de password por hashes reales generados con BCrypt si lo deseas.

