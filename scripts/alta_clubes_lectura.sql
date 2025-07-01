-- =====================================================
-- Script de Alta Masiva de Clubes de Lectura - Booky Backend
-- =====================================================

-- 1. INSERTAR ALGUNOS LIBROS PARA LOS CLUBES
INSERT INTO books (id, isbn, title, author, overview, pages, publisher, image) VALUES 
('book-001', '9780060935467', 'To Kill a Mockingbird', 'Harper Lee', 'A classic of modern American literature', 376, 'Harper Perennial', NULL),
('book-002', '9780747532699', 'Harry Potter and the Philosopher''s Stone', 'J.K. Rowling', 'The first book in the Harry Potter series', 223, 'Bloomsbury', NULL),
('book-003', '9780451524935', '1984', 'George Orwell', 'A dystopian social science fiction novel', 328, 'Signet Classics', NULL),
('book-004', '9780060850524', 'Brave New World', 'Aldous Huxley', 'A dystopian novel about a futuristic society', 268, 'Harper Perennial', NULL),
('book-005', '9780142000281', 'Of Mice and Men', 'John Steinbeck', 'A novella about two displaced migrant ranch workers', 112, 'Penguin Classics', NULL);

-- 2. INSERTAR CLUBES DE LECTURA
INSERT INTO reading_clubs (id, date_created, description, last_updated, name, book_id, community_id, moderator_id) VALUES 

-- Clubes en comunidades existentes
('club-001', NOW() - INTERVAL '1 month', 'Club dedicado a la lectura y análisis profundo de "To Kill a Mockingbird". Exploramos temas de justicia, racismo y crecimiento moral.', NOW() - INTERVAL '1 week', 'Club Mockingbird', 'book-001', 'comm-001', 'user-001'),

('club-002', NOW() - INTERVAL '3 weeks', 'Redescubriendo la magia de Harry Potter desde una perspectiva adulta. Analizamos simbolismos, referencias y el mundo mágico de Rowling.', NOW() - INTERVAL '3 days', 'Magia Adulta', 'book-002', 'comm-004', 'user-004'),

('club-003', NOW() - INTERVAL '2 weeks', 'Distopía orwelliana en tiempos modernos. ¿Qué tan cerca estamos del mundo de Winston Smith? Debate y reflexión crítica.', NOW() - INTERVAL '5 days', 'Hermano Mayor', 'book-003', 'comm-003', 'user-003'),

('club-004', NOW() - INTERVAL '1 week', 'Huxley vs Orwell: dos visiones del futuro. ¿Control por placer o por miedo? Comparamos ambas distopías.', NOW() - INTERVAL '2 days', 'Mundo Feliz', 'book-004', 'comm-007', 'user-007'),

('club-005', NOW() - INTERVAL '5 days', 'La amistad y los sueños rotos en la América de la Depresión. Steinbeck y su retrato crudo de la humanidad.', NOW() - INTERVAL '1 day', 'Ratones y Hombres', 'book-005', 'comm-005', 'user-005');

-- 3. MEMBRESÍAS DE CLUBES DE LECTURA
INSERT INTO reading_club_members (reading_club_id, user_id) VALUES 

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
('club-005', 'user-016');  -- Andrés (literatura latinoamericana)

-- 4. CATEGORÍAS DE LIBROS
INSERT INTO book_categories (book_id, category) VALUES 
('book-001', 'Fiction'), ('book-001', 'Classic Literature'), ('book-001', 'Social Issues'),
('book-002', 'Fantasy'), ('book-002', 'Young Adult'), ('book-002', 'Adventure'),
('book-003', 'Science Fiction'), ('book-003', 'Dystopian'), ('book-003', 'Political Fiction'),
('book-004', 'Science Fiction'), ('book-004', 'Dystopian'), ('book-004', 'Philosophy'),
('book-005', 'Fiction'), ('book-005', 'Classic Literature'), ('book-005', 'American Literature');

-- 5. CONSULTAS DE VERIFICACIÓN
SELECT 
    rc.name as club_name,
    b.title as book_title,
    b.author,
    c.name as community,
    u.username as moderator,
    COUNT(rcm.user_id) as total_members
FROM reading_clubs rc
JOIN books b ON rc.book_id = b.id
JOIN community c ON rc.community_id = c.id
JOIN users u ON rc.moderator_id = u.id
LEFT JOIN reading_club_members rcm ON rc.id = rcm.reading_club_id
GROUP BY rc.id, rc.name, b.title, b.author, c.name, u.username
ORDER BY total_members DESC;

-- Verificar libros con categorías
SELECT 
    b.title,
    b.author,
    STRING_AGG(bc.category, ', ') as categories
FROM books b
LEFT JOIN book_categories bc ON b.id = bc.book_id
GROUP BY b.id, b.title, b.author;

-- Usuarios más activos en clubes
SELECT 
    u.username,
    u.name,
    COUNT(rcm.reading_club_id) as clubes_participando
FROM users u
LEFT JOIN reading_club_members rcm ON u.id = rcm.user_id
GROUP BY u.id, u.username, u.name
HAVING COUNT(rcm.reading_club_id) > 0
ORDER BY clubes_participando DESC;

-- =====================================================
-- 6. NOTAS SOBRE IDs STRING
-- =====================================================
-- Ya no necesitamos manejar secuencias porque todas las tablas usan VARCHAR(255) como ID
-- Los nuevos libros tendrán IDs generados por la aplicación usando UUIDs o el patrón book-XXX 