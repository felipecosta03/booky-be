-- =====================================================
-- Script de Alta Masiva de Usuarios - Booky Backend
-- =====================================================
-- Este script permite crear usuarios y direcciones de forma masiva
-- Las contraseñas están encriptadas con BCrypt (strength 10)

-- =====================================================
-- 1. INSERTAR DIRECCIONES
-- =====================================================

INSERT INTO addresses (id, state, country, longitude, latitude) VALUES 
('addr-100', 'Buenos Aires', 'Argentina', -58.3816, -34.6037),
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

-- =====================================================
-- 2. INSERTAR USUARIOS
-- =====================================================
-- NOTA: Todas las contraseñas son "password123" encriptadas con BCrypt
-- Hash generado: $2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy

INSERT INTO users (id, email, username, password, name, lastname, description, image, coins, address_id, date_created) VALUES 

-- Usuarios Administradores
('admin-001', 'admin@booky.com', 'admin', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Administrador', 'Sistema', 'Cuenta de administrador del sistema', NULL, 1000, 'addr-100', NOW()),
('admin-002', 'superadmin@booky.com', 'superadmin', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Super', 'Administrador', 'Cuenta de super administrador', NULL, 2000, 'addr-100', NOW()),

-- Usuarios de Prueba - Argentina
('user-001', 'juan.perez@gmail.com', 'juanp', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Juan', 'Pérez', 'Amante de la literatura clásica y contemporánea', NULL, 150, 'addr-100', NOW()),
('user-002', 'maria.garcia@outlook.com', 'mariag', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'María', 'García', 'Lectora empedernida de novelas de misterio', NULL, 200, 'addr-101', NOW()),
('user-003', 'carlos.rodriguez@yahoo.com', 'carlosr', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Carlos', 'Rodríguez', 'Fanático de la ciencia ficción y fantasía', NULL, 175, 'addr-102', NOW()),
('user-004', 'ana.lopez@gmail.com', 'anal', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Ana', 'López', 'Especialista en literatura juvenil', NULL, 125, 'addr-103', NOW()),
('user-005', 'luis.martinez@hotmail.com', 'luism', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Luis', 'Martínez', 'Coleccionista de libros de historia', NULL, 300, 'addr-104', NOW()),
('user-006', 'sofia.gonzalez@gmail.com', 'sofiag', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Sofía', 'González', 'Aficionada a la poesía y narrativa', NULL, 180, 'addr-105', NOW()),
('user-007', 'diego.fernandez@yahoo.com', 'diegof', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Diego', 'Fernández', 'Lector de biografías y ensayos', NULL, 220, 'addr-106', NOW()),
('user-008', 'lucia.morales@outlook.com', 'luciam', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Lucía', 'Morales', 'Estudiante de literatura', NULL, 90, 'addr-107', NOW()),
('user-009', 'alejandro.silva@gmail.com', 'alejandros', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Alejandro', 'Silva', 'Escritor y crítico literario', NULL, 250, 'addr-108', NOW()),
('user-010', 'valentina.castro@hotmail.com', 'valentinac', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Valentina', 'Castro', 'Bloguera de reseñas de libros', NULL, 160, 'addr-109', NOW()),

-- Usuarios Internacionales
('user-011', 'isabella.martinez@gmail.es', 'isabellam', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Isabella', 'Martínez', 'Profesora de literatura española', NULL, 280, 'addr-110', NOW()),
('user-012', 'pablo.ruiz@outlook.es', 'pablor', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Pablo', 'Ruiz', 'Traductor literario', NULL, 320, 'addr-111', NOW()),
('user-013', 'fernanda.lopez@yahoo.mx', 'fernandal', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Fernanda', 'López', 'Editora de casa editorial', NULL, 400, 'addr-112', NOW()),
('user-014', 'ricardo.vargas@gmail.pe', 'ricardov', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Ricardo', 'Vargas', 'Bibliotecario y archivista', NULL, 190, 'addr-113', NOW()),
('user-015', 'camila.torres@outlook.cl', 'camilat', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Camila', 'Torres', 'Investigadora literaria', NULL, 230, 'addr-114', NOW()),
('user-016', 'andres.herrera@gmail.co', 'andresh', '$2a$10$hHkijCVJXJSNk6ZFXmO1y.q/ZL3J5YDLgVWOkbMzRzhXd1RPW1Eyy', 'Andrés', 'Herrera', 'Crítico de literatura latinoamericana', NULL, 270, 'addr-115', NOW());

-- =====================================================
-- 3. SECUENCIAS Y CONFIGURACIÓN
-- =====================================================

-- NOTA: Al usar String IDs, no necesitamos configurar secuencias
-- Los IDs se asignan manualmente o mediante UUID

-- =====================================================
-- 4. CONSULTAS DE VERIFICACIÓN
-- =====================================================

-- Verificar usuarios creados
SELECT 
    u.username,
    u.name,
    u.lastname,
    u.email,
    u.coins,
    a.state,
    a.country,
    u.date_created
FROM users u
JOIN addresses a ON u.address_id = a.id
ORDER BY u.date_created DESC;

-- Contar usuarios por país
SELECT 
    a.country,
    COUNT(*) as total_usuarios
FROM users u
JOIN addresses a ON u.address_id = a.id
GROUP BY a.country
ORDER BY total_usuarios DESC;

-- =====================================================
-- NOTAS IMPORTANTES:
-- =====================================================
-- 1. Todas las contraseñas son "password123" 
-- 2. Los IDs de usuarios usan formato: tipo-numero (ej: user-001, admin-001)
-- 3. Los coins iniciales varían según el tipo de usuario
-- 4. Las direcciones están distribuidas en varios países
-- 5. Para generar nuevos hashes BCrypt, usa: 
--    https://bcrypt-generator.com/ con strength 10 