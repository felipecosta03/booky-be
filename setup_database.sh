#!/bin/bash

# Script SIMPLE para setup completo de Booky - ARCHIVO UNIFICADO
echo "🚀 BOOKY - SETUP UNIFICADO"
echo "=========================="
echo ""

DB_NAME="postgres"
DB_USER="postgres"

echo "📋 Configuración:"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo ""

# Verificar que estamos en el directorio correcto
if [[ ! -f "pom.xml" || ! -f "booky_unified_setup.sql" ]]; then
    echo "❌ Error: Ejecutar desde la raíz del proyecto y asegurar que existe booky_unified_setup.sql"
    exit 1
fi

echo "🏗️ Usando archivo SQL unificado..."

# Verificar que el archivo unificado existe
if [[ ! -f "booky_unified_setup.sql" ]]; then
    echo "❌ Error: No se encuentra booky_unified_setup.sql"
    exit 1
fi

echo "✅ Archivo SQL unificado encontrado: booky_unified_setup.sql"
echo ""
echo "🎯 EJECUTAR (UNA SOLA CONTRASEÑA):"
echo "=================================="
echo ""
echo "psql -U $DB_USER -d $DB_NAME -f booky_unified_setup.sql"
echo ""
echo "📋 Este archivo unificado hace TODO en una sola ejecución:"
echo "  ✓ Limpia todas las tablas (elimina las no utilizadas)"
echo "  ✓ Crea esquema limpio (solo 18 tablas implementadas)"
echo "  ✓ Puebla todos los datos de prueba"
echo "  ✓ Configura gamificación completa"
echo "  ✓ Crea comunidades y clubes de lectura"
echo "  ✓ Inserta posts y comentarios"
echo "  ✓ Verifica el resultado final"
echo ""
echo "🔥 ¡Solo te pedirá la contraseña UNA VEZ!"
echo ""
echo "📊 BENEFICIOS DEL ARCHIVO UNIFICADO:"
echo "  ❌ 8 tablas eliminadas (no implementadas)"
echo "  ✅ 18 tablas activas (100% implementadas)"
echo "  📈 30% reducción en complejidad"
echo "  🚀 Mejor rendimiento y mantenimiento"
echo ""
echo "🎮 DESPUÉS DEL SETUP, PROBAR:"
echo "  • GET /gamification/profile/<user-id>"
echo "  • GET /exchanges (con JWT de user-001)"
echo "  • GET /communities"
echo "  • POST /exchanges (crear intercambio)"
echo ""
echo "👥 USUARIOS DE PRUEBA (password: password123):"
echo "  • user-001 (juan.perez@gmail.com) - Literatura clásica"
echo "  • user-002 (maria.garcia@outlook.com) - Misterio"
echo "  • user-003 (carlos.rodriguez@yahoo.com) - Sci-Fi"
echo "  • admin-001 (admin@booky.com) - Administrador"