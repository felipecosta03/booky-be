-- =====================================================
-- Script para arreglar limitaciones de VARCHAR(255) en libros
-- =====================================================

-- Aumentar límites de campos de libros que pueden ser largos
ALTER TABLE books 
    ALTER COLUMN title TYPE VARCHAR(1000),
    ALTER COLUMN publisher TYPE VARCHAR(500),
    ALTER COLUMN author TYPE VARCHAR(500),
    ALTER COLUMN image TYPE TEXT,
    ALTER COLUMN edition TYPE VARCHAR(500);

-- Aumentar límite de categorías que también pueden ser largas
ALTER TABLE book_categories 
    ALTER COLUMN category TYPE VARCHAR(500);

-- Verificar cambios
\d books;
\d book_categories; 