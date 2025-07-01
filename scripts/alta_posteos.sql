-- =====================================================
-- Script de Alta Masiva de Posteos - Booky Backend
-- =====================================================

-- 1. INSERTAR SEGUIDORES
INSERT INTO user_follows (follower_id, followed_id) VALUES 
('user-001', 'user-002'), ('user-001', 'user-003'), ('user-001', 'user-009'),
('user-002', 'user-001'), ('user-002', 'user-004'), ('user-002', 'user-010'),
('user-003', 'user-001'), ('user-003', 'user-002'), ('user-003', 'user-006'),
('user-004', 'user-008'), ('user-005', 'user-007'), ('user-006', 'user-009'),
('user-009', 'user-006'), ('user-010', 'user-002'), ('admin-001', 'user-009');

-- 2. POSTS GENERALES
INSERT INTO post (id, body, date_created, user_id, community_id) VALUES 
('post-001', '¡Acabo de terminar "Cien años de soledad"! La magia de García Márquez es incomparable. ¿Alguien más se quedó viviendo en Macondo? 📚✨', NOW() - INTERVAL '2 hours', 'user-001', NULL),
('post-002', 'Organizando mi biblioteca y tengo más libros sin leer que leídos. ¿Les pasa lo mismo? #ProblemasDeUnaLectora', NOW() - INTERVAL '5 hours', 'user-002', NULL),
('post-003', 'Debate: ¿Philip K. Dick o Isaac Asimov? Necesito opiniones para introducir a alguien al sci-fi.', NOW() - INTERVAL '8 hours', 'user-003', NULL),
('post-004', 'La literatura juvenil contemporánea aborda temas reales: ansiedad, identidad, justicia social. Está más madura que nunca.', NOW() - INTERVAL '12 hours', 'user-004', NULL),
('post-005', 'Encontré una biografía de Da Vinci con anotaciones del dueño anterior. Los libros usados tienen historias dentro de historias.', NOW() - INTERVAL '1 day', 'user-005', NULL),
('post-006', 'Escribí un poema inspirado en esta lluvia. La poesía está en todos lados, solo hay que estar dispuesto a verla. 🌧️', NOW() - INTERVAL '2 days', 'user-006', NULL);

-- 3. POSTS EN COMUNIDADES
INSERT INTO post (id, body, date_created, user_id, community_id) VALUES 
('post-007', '¿Es Emma Bovary víctima del romanticismo o adelantada a su tiempo? Flaubert creó un personaje complejo. Abramos el debate.', NOW() - INTERVAL '3 hours', 'user-001', 'comm-001'),
('post-008', '¿Alguien más piensa que Christie escribió el mejor final en "El asesinato de Roger Ackroyd"? Pura genialidad narrativa.', NOW() - INTERVAL '6 hours', 'user-002', 'comm-002'),
('post-009', '¿"Dune" es la mejor obra de sci-fi? La construcción del mundo de Herbert es increíble.', NOW() - INTERVAL '10 hours', 'user-003', 'comm-003'),
('post-010', 'Los libros YA abordan temas profundos. "The Hate U Give" habla de racismo con madurez impresionante.', NOW() - INTERVAL '4 hours', 'user-004', 'comm-004'),
('post-011', 'INTERCAMBIO: "Los pilares de la Tierra" por algo de Isabel Allende o Murakami. ¡CABA! 📚🔄', NOW() - INTERVAL '1 day', 'user-001', 'comm-019');

-- 4. COMENTARIOS
INSERT INTO comment (id, body, date_created, user_id, post_id) VALUES 
('comm-001', 'Macondo es más real que muchos lugares reales. García Márquez tenía esa magia única.', NOW() - INTERVAL '1 hour', 'user-002', 'post-001'),
('comm-002', '¡Ese es el síndrome del TBR infinito! Parte de la magia de ser lector.', NOW() - INTERVAL '4 hours', 'user-001', 'post-002'),
('comm-003', 'Dick para filosofía, Asimov para aventura. Para introducir: "Yo, robot" de Asimov.', NOW() - INTERVAL '7 hours', 'user-001', 'post-003'),
('comm-004', 'Emma es víctima de su época. Una mujer inteligente atrapada en un mundo limitante.', NOW() - INTERVAL '2 hours', 'user-006', 'post-007'),
('comm-005', '¡SÍ! Ese final me rompió la cabeza. Christie era maestra del plot twist.', NOW() - INTERVAL '5 hours', 'user-001', 'post-008'),
('comm-006', '¡Tengo "La casa de los espíritus"! ¿Te interesa?', NOW() - INTERVAL '18 hours', 'user-005', 'post-011');

-- 5. VERIFICACIÓN
SELECT 'Posts creados:' as info, COUNT(*) as total FROM post
UNION ALL
SELECT 'Comentarios creados:', COUNT(*) FROM comment
UNION ALL
SELECT 'Relaciones de seguimiento:', COUNT(*) FROM user_follows; 