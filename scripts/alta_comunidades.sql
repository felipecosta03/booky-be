-- =====================================================
-- Script de Alta Masiva de Comunidades - Booky Backend
-- =====================================================
-- Este script crea comunidades con los usuarios existentes como administradores

-- =====================================================
-- 1. INSERTAR COMUNIDADES
-- =====================================================

INSERT INTO community (id, date_created, description, name, admin_id) VALUES 

-- Comunidades Literarias Generales
('comm-001', NOW(), 'Espacio para amantes de la literatura clásica y contemporánea. Compartimos reseñas, recomendaciones y debates sobre grandes obras.', 'Literatura Clásica', 'user-001'),
('comm-002', NOW(), 'Comunidad dedicada a los misterios más intrigantes de la literatura. Desde Agatha Christie hasta autores contemporáneos.', 'Club de Misterio', 'user-002'),
('comm-003', NOW(), 'Para fanáticos de mundos fantásticos, naves espaciales y criaturas míticas. Ciencia ficción y fantasía sin límites.', 'Sci-Fi & Fantasy', 'user-003'),
('comm-004', NOW(), 'Descubre las mejores historias para jóvenes lectores. Desde clásicos juveniles hasta nuevas tendencias.', 'Literatura Juvenil', 'user-004'),
('comm-005', NOW(), 'Exploramos el pasado a través de biografías, ensayos históricos y documentos que marcaron épocas.', 'Historia y Biografías', 'user-005'),

-- Comunidades por Géneros Específicos
('comm-006', NOW(), 'Versos que tocan el alma. Compartimos poesía clásica, contemporánea y creaciones propias.', 'Poesía Universal', 'user-006'),
('comm-007', NOW(), 'Reflexiones profundas, ensayos filosóficos y textos que nos hacen pensar diferente.', 'Ensayos y Filosofía', 'user-007'),
('comm-008', NOW(), 'Para estudiantes y académicos. Analizamos textos, compartimos recursos y discutimos técnicas literarias.', 'Análisis Literario', 'user-008'),
('comm-009', NOW(), 'Escritores, críticos y editores compartiendo experiencias del mundo editorial y creativo.', 'Escritores Unidos', 'user-009'),
('comm-010', NOW(), 'Reseñas honestas, recomendaciones personalizadas y debates sobre las últimas publicaciones.', 'Reseñas y Críticas', 'user-010'),

-- Comunidades Regionales
('comm-011', NOW(), 'Celebramos la rica tradición literaria de España, desde el Siglo de Oro hasta la actualidad.', 'Literatura Española', 'user-011'),
('comm-012', NOW(), 'Traducción como arte: compartimos técnicas, desafíos y bellezas de llevar textos entre idiomas.', 'Arte de Traducir', 'user-012'),
('comm-013', NOW(), 'Para profesionales del mundo editorial: editores, correctores, diseñadores y distribuidores.', 'Industria Editorial', 'user-013'),
('comm-014', NOW(), 'Bibliotecarios, archivistas y custodios del conocimiento compartiendo recursos y experiencias.', 'Guardianes del Saber', 'user-014'),
('comm-015', NOW(), 'Investigación académica en literatura: metodologías, proyectos y descubrimientos.', 'Investigación Literaria', 'user-015'),
('comm-016', NOW(), 'La voz única de América Latina: desde Borges hasta los nuevos talentos emergentes.', 'Literatura Latinoamericana', 'user-016'),

-- Comunidades Temáticas Especiales
('comm-017', NOW(), 'Libros que nos ayudan a crecer, mejorar y encontrar nuestro camino en la vida.', 'Desarrollo Personal', 'admin-001'),
('comm-018', NOW(), 'Novelas gráficas, cómics y literatura visual. El arte de contar historias con imágenes.', 'Novela Gráfica', 'admin-002'),
('comm-019', NOW(), 'Intercambio físico de libros entre miembros. Dale una segunda vida a tus lecturas.', 'Intercambio de Libros', 'user-001'),
('comm-020', NOW(), 'Desafíos de lectura mensuales, maratones literarios y metas compartidas.', 'Desafíos Lectores', 'user-002');

-- =====================================================
-- 2. INSERTAR MEMBRESÍAS DE COMUNIDADES
-- =====================================================

-- Los administradores son automáticamente miembros de sus comunidades
INSERT INTO community_members (community_id, user_id) VALUES 
('comm-001', 'user-001'),
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
INSERT INTO community_members (community_id, user_id) VALUES 
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
('comm-003', 'admin-001'),  -- Admin en Sci-Fi & Fantasy
('comm-009', 'admin-001'),  -- Admin en Escritores Unidos
('comm-001', 'admin-002'),  -- Super Admin en Literatura Clásica
('comm-010', 'admin-002');  -- Super Admin en Reseñas y Críticas

-- =====================================================
-- 3. CONSULTAS DE VERIFICACIÓN
-- =====================================================

-- Verificar comunidades creadas
SELECT 
    c.name,
    c.description,
    u.username as admin_username,
    u.name as admin_name,
    c.date_created
FROM community c
JOIN users u ON c.admin_id = u.id
ORDER BY c.date_created DESC;

-- Contar miembros por comunidad
SELECT 
    c.name as comunidad,
    COUNT(cm.user_id) as total_miembros
FROM community c
LEFT JOIN community_members cm ON c.id = cm.community_id
GROUP BY c.id, c.name
ORDER BY total_miembros DESC;

-- Mostrar comunidades más populares
SELECT 
    c.name as comunidad,
    c.description,
    u.username as admin,
    COUNT(cm.user_id) as miembros
FROM community c
JOIN users u ON c.admin_id = u.id
LEFT JOIN community_members cm ON c.id = cm.community_id
GROUP BY c.id, c.name, c.description, u.username
HAVING COUNT(cm.user_id) >= 2
ORDER BY miembros DESC, c.name;

-- =====================================================
-- ESTADÍSTICAS ÚTILES
-- =====================================================

-- Total de comunidades por administrador
SELECT 
    u.username,
    u.name,
    COUNT(c.id) as comunidades_administradas
FROM users u
LEFT JOIN community c ON u.id = c.admin_id
GROUP BY u.id, u.username, u.name
HAVING COUNT(c.id) > 0
ORDER BY comunidades_administradas DESC;

-- Usuarios más activos (en más comunidades)
SELECT 
    u.username,
    u.name,
    COUNT(cm.community_id) as comunidades_participando
FROM users u
LEFT JOIN community_members cm ON u.id = cm.user_id
GROUP BY u.id, u.username, u.name
HAVING COUNT(cm.community_id) > 0
ORDER BY comunidades_participando DESC, u.username; 