# 🚀 Booky Web Test Interface

Interfaz de prueba rápida para testear las funcionalidades de la API de Booky.

## 🎯 Cómo Usar

1. **Asegúrate de que el backend esté ejecutándose:**
   ```bash
   ./start.sh
   ```

2. **Abre la interfaz:**
   ```bash
   open web/index.html
   # o simplemente abre el archivo en tu navegador
   ```

3. **Inicia sesión:**
   - Selecciona cualquier usuario del dropdown
   - Todos tienen la contraseña: `password123`
   - Haz clic en "Login"

## 🔧 Funcionalidades Disponibles

### 📝 Posts
- **Mi Feed**: Ver posts de usuarios que sigues
- **Todos los Posts**: Ver todos los posts públicos
- **Crear Post**: Publicar nuevo post (general o en comunidad)
- **Ver Comentarios**: Expandir comentarios de cada post
- **Eliminar Post**: Solo tus propios posts

### 🏘️ Comunidades  
- **Todas las Comunidades**: Explorar 20 comunidades temáticas
- **Mis Comunidades**: Ver comunidades donde eres miembro
- **Ver Posts**: Posts específicos de cada comunidad
- **Unirse**: Funcionalidad básica (placeholder)

### 📖 Clubes de Lectura
- **Todos los Clubes**: Ver 5 clubes con libros específicos
- **Mis Clubes**: Clubes donde participas
- **Ver Libro**: Detalles del libro del club
- **Unirse**: Funcionalidad básica (placeholder)

### 👥 Usuarios
- **Todos los Usuarios**: Ver 18 usuarios de prueba
- **Seguir Usuario**: Funcionalidad básica (placeholder)
- **Ver Posts**: Posts de usuario específico
- **Información**: Coins, ubicación, fecha de registro

## 🎨 Características

- **Design Moderno**: Interfaz limpia con gradientes y animaciones
- **Responsive**: Funciona en desktop y móvil
- **Real-time**: Conecta directamente con tu API local
- **Datos Reales**: Usa los datos creados por los scripts de seed

## 🛠️ Usuarios de Prueba

- `admin` - Administrador del sistema
- `juanp` - Juan Pérez (Literatura clásica)
- `mariag` - María García (Misterio) 
- `carlosr` - Carlos Rodríguez (Sci-Fi)
- `anal` - Ana López (Literatura juvenil)
- `luism` - Luis Martínez (Historia)
- `sofiag` - Sofía González (Poesía)
- Y más...

## 🔍 Para Probar

1. **Login como `juanp`** → Ver su feed personalizado
2. **Cambiar a tab Comunidades** → Explorar "Literatura Clásica"
3. **Crear un post** → Publicar en comunidad específica
4. **Ver comentarios** → Expandir conversaciones
5. **Cambiar usuario** → Login como `mariag` y ver diferencias

## 📋 Endpoints Testados

- `POST /auth/login` - Autenticación
- `GET /posts` - Todos los posts
- `GET /posts/feed` - Feed personalizado
- `POST /posts` - Crear post
- `GET /posts/{id}/comments` - Comentarios
- `GET /communities` - Comunidades
- `GET /reading-clubs` - Clubes de lectura
- `GET /users` - Usuarios

## 🗑️ Para Borrar

Simplemente elimina la carpeta `web/` cuando termines de probar.

## ⚠️ Limitaciones

- Algunas funciones son placeholders (seguir usuarios, unirse a comunidades)
- No hay autenticación persistente (se pierde al refrescar)
- No hay manejo de imágenes en posts
- Diseñado para pruebas rápidas, no producción

¡Listo para probar todas las funcionalidades de Booky! 🎉 