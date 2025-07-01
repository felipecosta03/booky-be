# Scripts de Seed de Datos - Booky Backend

## Scripts Disponibles

- `alta_usuarios.sql` - 18 usuarios + direcciones (contraseña: `password123`)
- `alta_comunidades.sql` - 20 comunidades literarias + membresías  
- `alta_posteos.sql` - Posts, comentarios, seguidores
- `alta_clubes_lectura.sql` - 5 libros + 5 clubes de lectura
- `seed_all_data.sql` - Script maestro que ejecuta todos

## Ejecución Automática

Los scripts se ejecutan automáticamente con Docker:

```bash
./start.sh
```

## Datos Creados

- **18 usuarios** (admin/superadmin + 16 regulares)
- **20 comunidades** literarias temáticas
- **50+ membresías** entre usuarios y comunidades
- **Posts y comentarios** realistas
- **Red de seguidores** para feed social  
- **5 clubes de lectura** con libros clásicos

## Verificación

```sql
SELECT 'USUARIOS' as tabla, COUNT(*) FROM users
UNION SELECT 'COMUNIDADES', COUNT(*) FROM community
UNION SELECT 'POSTS', COUNT(*) FROM post; 