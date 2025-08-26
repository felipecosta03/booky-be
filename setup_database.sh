#!/bin/bash

# Script SIMPLE para setup completo de Booky - UNA SOLA CONTRASEÑA
echo "🚀 BOOKY - SETUP COMPLETO"
echo "========================="
echo ""

DB_NAME="postgres"
DB_USER="postgres"

echo "📋 Configuración:"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Verificar que estamos en el directorio correcto
if [[ ! -f "pom.xml" || ! -d "scripts" ]]; then
    echo "❌ Error: Ejecutar desde la raíz del proyecto"
    exit 1
fi

echo "🏗️ Creando script SQL completo..."

# Crear un único script SQL que haga todo
cat > booky_complete_setup.sql << 'EOF'
-- =====================================================
-- BOOKY - SETUP COMPLETO EN UNA SOLA EJECUCIÓN
-- =====================================================

\echo '🚀 SETUP COMPLETO DE BOOKY'
\echo '=========================='

-- Configurar para parar en errores
\set ON_ERROR_STOP on

-- =====================================================
-- PASO 1: LIMPIEZA COMPLETA
-- =====================================================
\echo '🗑️ PASO 1: Eliminando todas las tablas...'

DROP TABLE IF EXISTS user_achievements CASCADE;
DROP TABLE IF EXISTS gamification_profiles CASCADE;
DROP TABLE IF EXISTS achievements CASCADE;
DROP TABLE IF EXISTS user_levels CASCADE;
DROP TABLE IF EXISTS community_members CASCADE;
DROP TABLE IF EXISTS reading_club_members CASCADE;
DROP TABLE IF EXISTS reading_clubs CASCADE;
DROP TABLE IF EXISTS comment CASCADE;
DROP TABLE IF EXISTS post CASCADE;
DROP TABLE IF EXISTS community CASCADE;
DROP TABLE IF EXISTS user_books CASCADE;
DROP TABLE IF EXISTS book_categories CASCADE;
DROP TABLE IF EXISTS books CASCADE;
DROP TABLE IF EXISTS user_follows CASCADE;
DROP TABLE IF EXISTS user_rates CASCADE;
DROP TABLE IF EXISTS book_transaction CASCADE;
DROP TABLE IF EXISTS transaction_receiver_books CASCADE;
DROP TABLE IF EXISTS transaction_sender_books CASCADE;
DROP TABLE IF EXISTS community_likes CASCADE;
DROP TABLE IF EXISTS user_activity_badges CASCADE;
DROP TABLE IF EXISTS user_activity CASCADE;
DROP TABLE IF EXISTS badge CASCADE;
DROP TABLE IF EXISTS level CASCADE;
DROP TABLE IF EXISTS author CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS addresses CASCADE;

\echo '✅ Tablas eliminadas'

-- =====================================================
-- PASO 2: ESQUEMAS PRINCIPALES
-- =====================================================
\echo ''
\echo '🏗️ PASO 2: Creando esquemas principales...'

\i scripts/database_schema_updated.sql

-- =====================================================
-- PASO 3: ESQUEMAS DE GAMIFICACIÓN
-- =====================================================
\echo ''
\echo '🎮 PASO 3: Creando esquemas de gamificación...'

CREATE TABLE user_levels (
    level INTEGER PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    min_points INTEGER NOT NULL,
    max_points INTEGER NOT NULL,
    badge VARCHAR(100),
    color VARCHAR(7)
);

CREATE TABLE achievements (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    icon VARCHAR(100),
    required_value INTEGER NOT NULL,
    condition_type VARCHAR(50) NOT NULL,
    points_reward INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE gamification_profiles (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL UNIQUE,
    total_points INTEGER NOT NULL DEFAULT 0,
    current_level INTEGER NOT NULL DEFAULT 1,
    books_read INTEGER NOT NULL DEFAULT 0,
    exchanges_completed INTEGER NOT NULL DEFAULT 0,
    posts_created INTEGER NOT NULL DEFAULT 0,
    comments_created INTEGER NOT NULL DEFAULT 0,
    communities_joined INTEGER NOT NULL DEFAULT 0,
    communities_created INTEGER NOT NULL DEFAULT 0,
    reading_clubs_joined INTEGER NOT NULL DEFAULT 0,
    reading_clubs_created INTEGER NOT NULL DEFAULT 0,
    last_activity TIMESTAMP,
    date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (current_level) REFERENCES user_levels(level)
);

CREATE TABLE user_achievements (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    achievement_id VARCHAR(50) NOT NULL,
    date_earned TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notified BOOLEAN NOT NULL DEFAULT false,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements(id) ON DELETE CASCADE,
    UNIQUE(user_id, achievement_id)
);

\echo '✅ Esquemas de gamificación creados'

-- =====================================================
-- PASO 4: DATOS DE GAMIFICACIÓN
-- =====================================================
\echo ''
\echo '🏆 PASO 4: Insertando datos de gamificación...'

INSERT INTO user_levels (level, name, description, min_points, max_points, badge, color) VALUES
(1, 'Novato', 'Recién comenzando tu aventura literaria', 0, 99, '🌱', '#28a745'),
(2, 'Aprendiz', 'Empezando a descubrir el mundo de los libros', 100, 249, '📚', '#17a2b8'),
(3, 'Lector', 'Ya tienes experiencia con los libros', 250, 499, '🤓', '#6f42c1'),
(4, 'Bibliófilo', 'Amante verdadero de la literatura', 500, 999, '📖', '#fd7e14'),
(5, 'Experto', 'Conocedor profundo del mundo literario', 1000, 1999, '🎓', '#e83e8c'),
(6, 'Maestro', 'Referente en la comunidad de lectores', 2000, 3999, '👑', '#dc3545'),
(7, 'Leyenda', 'El más alto honor en Booky', 4000, 999999, '⭐', '#ffc107');

INSERT INTO achievements (id, name, description, category, icon, required_value, condition_type, points_reward, is_active) VALUES
('ach-first-book', 'Primer Libro', 'Agrega tu primer libro a la biblioteca', 'LECTOR', '📚', 1, 'BOOKS_READ', 25, true),
('ach-reader-novice', 'Lector Novato', 'Lee 5 libros', 'LECTOR', '📖', 5, 'BOOKS_READ', 50, true),
('ach-first-exchange', 'Primer Intercambio', 'Completa tu primer intercambio', 'INTERCAMBIO', '🤝', 1, 'EXCHANGES_COMPLETED', 75, true),
('ach-first-post', 'Primera Palabra', 'Escribe tu primer post', 'SOCIAL', '💬', 1, 'POSTS_CREATED', 25, true),
('ach-leader', 'Líder', 'Crea una comunidad', 'SOCIAL', '👑', 1, 'COMMUNITIES_CREATED', 200, true),
('ach-club-member', 'Nuevo Miembro', 'Únete a tu primer club de lectura', 'CLUB', '📖', 1, 'READING_CLUBS_JOINED', 50, true),
('ach-hundred-points', 'Centurión', 'Alcanza 100 puntos', 'HITO', '💯', 100, 'TOTAL_POINTS', 25, true);

\echo '✅ Datos de gamificación insertados'

-- =====================================================
-- PASO 5: DATOS EXISTENTES
-- =====================================================
\echo ''
\echo '👥 PASO 5: Insertando usuarios...'
\i scripts/alta_usuarios.sql

\echo ''
\echo '📚 PASO 6: Insertando libros...'
\i scripts/alta_libros_userbooks.sql

\echo ''
\echo '🎮 PASO 7: Creando perfiles de gamificación...'
INSERT INTO gamification_profiles (id, user_id, total_points, current_level, books_read, exchanges_completed, posts_created, comments_created, communities_joined, communities_created, reading_clubs_joined, reading_clubs_created, date_created)
SELECT 
    'gp-' || u.id,
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

\echo ''
\echo '🏘️ PASO 8: Insertando comunidades...'
\i scripts/alta_comunidades.sql

\echo ''
\echo '📖 PASO 9: Insertando clubes de lectura...'
\i scripts/alta_clubes_lectura.sql

\echo ''
\echo '💬 PASO 10: Insertando posts...'
\i scripts/alta_posteos.sql

-- =====================================================
-- VERIFICACIÓN FINAL
-- =====================================================
\echo ''
\echo '🔍 VERIFICACIÓN FINAL'
\echo '===================='

SELECT 'Users: ' || COUNT(*) FROM users
UNION ALL
SELECT 'Books: ' || COUNT(*) FROM books
UNION ALL
SELECT 'Communities: ' || COUNT(*) FROM community
UNION ALL
SELECT 'User_levels: ' || COUNT(*) FROM user_levels
UNION ALL
SELECT 'Achievements: ' || COUNT(*) FROM achievements
UNION ALL
SELECT 'Gamification_profiles: ' || COUNT(*) FROM gamification_profiles;

\echo ''
\echo 'Verificando primeros usuarios:'
SELECT 'User ID: ' || id || ' - Email: ' || email FROM users LIMIT 3;

\echo ''
\echo '🎉 ¡SETUP COMPLETADO!'
\echo '=================='
\echo '✅ Todas las tablas creadas'
\echo '✅ Todos los datos poblados' 
\echo '✅ Sistema de gamificación listo'
\echo ''
\echo '🎮 Probar: GET /gamification/profile/<user-id>'

COMMIT;
EOF

echo "✅ Script SQL completo generado: booky_complete_setup.sql"
echo ""
echo "🎯 EJECUTAR (UNA SOLA CONTRASEÑA):"
echo "=================================="
echo ""
echo "psql -U $DB_USER -d $DB_NAME -f booky_complete_setup.sql"
echo ""
echo "📋 Este script hace TODO en una sola conexión:"
echo "  ✓ Limpia todas las tablas"
echo "  ✓ Crea todos los esquemas"
echo "  ✓ Puebla todos los datos"
echo "  ✓ Verifica el resultado"
echo ""
echo "🔥 ¡Solo te pedirá la contraseña UNA VEZ!"