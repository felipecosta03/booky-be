-- =====================================================
-- Script Maestro de Seed de Datos - Booky Backend
-- =====================================================
-- Este script ejecuta todos los seeds en el orden correcto

-- Orden de ejecución:
-- 1. Usuarios y direcciones (ya existe en alta_usuarios.sql)
-- 2. Comunidades y membresías  
-- 3. Posts, comentarios y seguidores
-- 4. Libros y clubes de lectura

\echo '=== INICIANDO SEED DE DATOS BOOKY ==='

-- 1. USUARIOS Y DIRECCIONES
\echo '--- Ejecutando alta_usuarios.sql ---'
\i /docker-entrypoint-initdb.d/scripts/alta_usuarios.sql

-- 2. COMUNIDADES Y MEMBRESÍAS
\echo '--- Ejecutando alta_comunidades.sql ---'
\i /docker-entrypoint-initdb.d/scripts/alta_comunidades.sql

-- 3. POSTS Y ACTIVIDAD SOCIAL
\echo '--- Ejecutando alta_posteos.sql ---'
\i /docker-entrypoint-initdb.d/scripts/alta_posteos.sql

-- 4. CLUBES DE LECTURA
\echo '--- Ejecutando alta_clubes_lectura.sql ---'
\i /docker-entrypoint-initdb.d/scripts/alta_clubes_lectura.sql

-- 5. ESTADÍSTICAS FINALES
\echo '=== ESTADÍSTICAS FINALES ==='

SELECT 'USUARIOS' as tabla, COUNT(*) as total FROM users
UNION ALL
SELECT 'DIRECCIONES', COUNT(*) FROM addresses
UNION ALL
SELECT 'COMUNIDADES', COUNT(*) FROM community
UNION ALL
SELECT 'MEMBRESÍAS COMUNIDADES', COUNT(*) FROM community_members
UNION ALL
SELECT 'POSTS', COUNT(*) FROM post
UNION ALL
SELECT 'COMENTARIOS', COUNT(*) FROM comment
UNION ALL
SELECT 'SEGUIDORES', COUNT(*) FROM user_follows
UNION ALL
SELECT 'LIBROS', COUNT(*) FROM books
UNION ALL
SELECT 'CLUBES LECTURA', COUNT(*) FROM reading_clubs
UNION ALL
SELECT 'MEMBRESÍAS CLUBES', COUNT(*) FROM reading_club_members;

\echo '=== SEED COMPLETADO EXITOSAMENTE ===' 