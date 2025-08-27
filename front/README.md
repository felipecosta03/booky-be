# 🚀 Booky Frontend - Test Application

Frontend de prueba para testear todas las funcionalidades de la API de Booky, incluyendo el sistema de gamificación.

## 🎯 Características

### ✅ **TODAS LAS FUNCIONALIDADES IMPLEMENTADAS**

#### **🔐 Autenticación & Sesión**
- Login completo con JWT
- Manejo de tokens y sesión persistente
- Logout seguro con limpieza de estado

#### **🎮 Sistema de Gamificación COMPLETO**
- **Panel de progreso** visual con nivel y puntos actuales
- **7 Niveles**: Novato → Aprendiz → Lector → Bibliófilo → Experto → Maestro → Leyenda
- **7 Logros**: Primer Libro, Lector Novato, Primer Intercambio, Primera Palabra, Líder, Nuevo Miembro, Centurión
- **Barra de progreso** animada hacia el siguiente nivel
- **Actualización automática** cada 30 segundos
- **Badge dinámico** en header con nivel y puntos en tiempo real

#### **📚 Gestión de Libros AVANZADA**
- **Agregar libros** con título, autor, ISBN, estado
- **Estados dinámicos**: Por Leer, Leyendo, Leído
- **Favoritos**: Marcar libros como favoritos ⭐
- **Intercambios**: Activar/desactivar disponibilidad para intercambio
- **Filtros**: Por estado y disponibilidad de intercambio
- **Preferencias de intercambio**: Especificar qué libros se buscan a cambio
- **Vista detallada** de cada libro
- **Búsqueda de libros** (preparado para Google Books API)

#### **👥 Sistema Social COMPLETO**
- **Descubrir usuarios**: Ver todos los usuarios registrados
- **Seguir/Dejar de seguir**: Sistema completo de follows
- **Buscar usuarios**: Por nombre o email
- **Perfiles de usuario**: Ver biblioteca, estadísticas y logros de otros
- **Sugerencias**: Usuarios recomendados basados en actividad
- **Contadores**: Seguidores y siguiendo en tiempo real
- **Avatars dinámicos**: Con iniciales de nombres

#### **🏘️ Comunidades AVANZADAS**
- **Crear comunidades** con nombre y descripción
- **Unirse/Salir** de comunidades
- **Mis comunidades**: Ver solo las que soy miembro
- **Ver posts por comunidad**: Filtrar contenido
- **Estadísticas**: Número de miembros, fecha de creación
- **Administración**: Ver quién es el admin de cada comunidad

#### **📖 Clubes de Lectura COMPLETOS**
- **Crear clubes** asociados a comunidades y libros específicos
- **Unirse a clubes** de lectura
- **Libro actual**: Ver qué libro está leyendo el club
- **Moderadores**: Sistema de moderación
- **Miembros**: Ver participantes del club
- **Mis clubes**: Gestionar participación personal

#### **💬 Posts & Comentarios COMPLETOS**
- **Crear posts** en comunidades específicas
- **Ver todos los posts** o filtrar por comunidad
- **Sistema de comentarios** completo
- **Comentar posts**: Agregar y ver comentarios
- **Metadata completa**: Autor, fecha, comunidad
- **Interacción social**: Ver discusiones en tiempo real

#### **🤝 Sistema de Intercambios AVANZADO**
- **Ver intercambios**: Propuestas recibidas y enviadas
- **Estados detallados**: Pendiente, Aceptado, Rechazado, Completado
- **Participantes**: Ver quién propone y quién recibe
- **Libros involucrados**: Lista completa de libros en cada intercambio
- **Mensajes**: Comunicación entre las partes
- **Acciones**: Aceptar, rechazar intercambios
- **Historial completo**: Todos los intercambios realizados

#### **👤 Perfil Personal DETALLADO**
- **Información completa**: Nombre, email, ubicación, descripción
- **Estadísticas visuales**: Libros totales, leídos, comunidades, intercambios
- **Gestión de cuenta**: Editar perfil, cambiar contraseña
- **Avatar personalizado**: Con iniciales del usuario
- **Historial de actividad**: Fecha de registro y actividad

## 🛠️ Tecnologías Utilizadas

- **HTML5** - Estructura semántica
- **CSS3** - Diseño moderno con gradientes y animaciones
- **JavaScript (Vanilla)** - Lógica de la aplicación
- **Font Awesome** - Iconos
- **Fetch API** - Comunicación con backend
- **CSS Grid/Flexbox** - Layout responsivo

## 🎨 Diseño

### **🎨 Tema Visual**
- **Gradientes modernos** en tonos púrpura/azul
- **Cards con glassmorphism** y blur effects
- **Animaciones suaves** y transiciones
- **Responsive design** para móviles y desktop
- **Sistema de notificaciones** tipo toast

### **🏆 Gamificación UI**
- **Panel destacado** con progreso visual
- **Círculo de nivel** con diseño atractivo
- **Grid de logros** con estados earned/pending
- **Colores temáticos** para cada tipo de logro
- **Badges informativos** en tiempo real

## 🚀 Cómo Usar

### **1. Prerequisitos**
```bash
# Asegúrate de que tu backend esté corriendo
./mvnw spring-boot:run

# Backend debe estar en: http://localhost:8080
```

### **2. Abrir Frontend**
```bash
# Desde la carpeta del proyecto
cd front

# Abrir en navegador (cualquier método)
open index.html
# O dobble-click en index.html
# O usar un servidor local como Live Server
```

### **3. Login**
Usa cualquiera de estos usuarios de prueba:

```
Email: juan.perez@example.com
Password: password123

Email: maria.garcia@example.com  
Password: password123

Email: carlos.rodriguez@example.com
Password: password123
```

## 📱 Funcionalidades por Sección

### **🏠 Dashboard Principal**
- **Header dinámico** con badge de gamificación y stats sociales
- **Panel de progreso** con nivel, puntos y barra de progreso
- **Grid de logros** con estado earned/pending
- **Navegación por 7 tabs** entre todas las secciones

### **📚 Sección Libros**
- **Biblioteca personal** con todos los libros del usuario
- **Agregar libros** con formulario completo (título, autor, ISBN, estado, favorito, intercambio)
- **Filtros avanzados** por estado y disponibilidad de intercambio
- **Cambiar estados** dinámicamente con dropdown
- **Gestión de intercambios** activar/desactivar por libro
- **Vista de grid** responsiva con información completa
- **Búsqueda de libros** preparada para integración con Google Books

### **🏘️ Sección Comunidades**
- **Todas las comunidades** disponibles en la plataforma
- **Crear nuevas comunidades** con nombre y descripción
- **Unirse/Salir** de comunidades con un click
- **Ver posts por comunidad** con navegación directa
- **Mis comunidades** filtro para ver solo participación personal
- **Estadísticas** de miembros y fecha de creación

### **📖 Sección Clubes de Lectura**
- **Todos los clubes** de lectura disponibles
- **Crear clubes** asociados a comunidades específicas y libros
- **Unirse a clubes** de interés
- **Ver libro actual** que está leyendo cada club
- **Información de moderadores** y número de miembros
- **Mis clubes** para gestionar participación personal

### **💬 Sección Posts & Comentarios**
- **Feed completo** de posts de todas las comunidades
- **Filtrar por comunidad** específica
- **Crear nuevos posts** en comunidades donde soy miembro
- **Sistema de comentarios** completo con respuestas
- **Ver detalles** de posts con comentarios
- **Interacción social** en tiempo real

### **🤝 Sección Intercambios**
- **Intercambios recibidos y enviados** con participantes visibles
- **Estados detallados** con colores (Pendiente, Aceptado, Rechazado, Completado)
- **Libros involucrados** en cada intercambio
- **Mensajes** entre participantes
- **Acciones rápidas** aceptar/rechazar desde la interfaz
- **Historial completo** de todos los intercambios

### **👥 Sección Usuarios**
- **Descubrir usuarios** con grid de usuarios sugeridos
- **Buscar usuarios** por nombre o email
- **Sistema de follows** completo (seguir/dejar de seguir)
- **Ver perfil completo** de otros usuarios con sus bibliotecas
- **Secciones organizadas**: Sugeridos, Siguiendo, Seguidores
- **Perfiles modales** con información detallada y libros públicos

### **👤 Sección Mi Perfil**
- **Información personal** completa (nombre, email, username, descripción)
- **Avatar dinámico** con iniciales personalizadas
- **Estadísticas visuales**: Libros totales, leídos, comunidades, intercambios
- **Gestión de cuenta**: Editar perfil y cambiar contraseña
- **Historial de actividad** y fecha de registro

## 🎮 Testing de Gamificación

### **📊 Acciones que Generan Puntos**

1. **📚 Agregar primer libro** → +25 pts (Logro: "Primer Libro")
2. **👁️ Leer 5 libros** → +50 pts (Logro: "Lector Novato")  
3. **🏘️ Crear comunidad** → +200 pts (Logro: "Líder")
4. **👥 Unirse a comunidad** → +50 pts
5. **💬 Crear primer post** → +25 pts (Logro: "Primera Palabra")
6. **🤝 Completar intercambio** → +75 pts (Logro: "Primer Intercambio")
7. **💯 Alcanzar 100 puntos** → +25 pts (Logro: "Centurión")

### **🔄 Testing Automático**
- **Actualización cada 30 segundos** del progreso
- **Notificaciones toast** en cada acción
- **Sincronización** entre badge y panel
- **Persistencia** de progreso

## 🔧 Configuración

### **API Endpoint**
```javascript
// En app.js, línea 2
const API_BASE_URL = 'http://localhost:8080';

// Cambiar si tu backend corre en otro puerto
```

### **Usuarios de Prueba**
Los usuarios están predefinidos en el HTML para facilitar testing. Puedes cambiarlos en `index.html` líneas 51-52.

## 🐛 Debugging

### **Consola del Navegador**
- Todas las llamadas a la API se loggean
- Errores se muestran con detalles
- Estado de gamificación se actualiza en consola

### **Red/Network Tab**
- Verificar requests HTTP
- Ver responses de la API
- Debuggear problemas de CORS

### **Problemas Comunes**

1. **CORS Error**: Asegúrate de que CorsConfig permita localhost
2. **401 Unauthorized**: Token expiró, hacer login nuevamente  
3. **500 Server Error**: Verificar logs del backend
4. **Data no aparece**: Verificar que se ejecutó el setup de BD

## 📈 Próximas Funcionalidades

- [ ] **Sistema de comentarios** completo
- [ ] **Búsqueda de libros** con Google Books API
- [ ] **Perfil de usuario** editable
- [ ] **Notificaciones en tiempo real** 
- [ ] **Modo oscuro**
- [ ] **PWA capabilities**
- [ ] **Intercambios avanzados** con negociación

## 🎯 Testing Checklist

Para verificar que todo funciona:

- [ ] ✅ Login exitoso con usuario de prueba
- [ ] ✅ Panel de gamificación se carga correctamente
- [ ] ✅ Agregar libro genera puntos y actualiza progreso
- [ ] ✅ Crear comunidad otorga logro "Líder" (+200 pts)
- [ ] ✅ Unirse a comunidad genera puntos
- [ ] ✅ Cambiar estado de libro a "Leído" genera puntos
- [ ] ✅ Badges y progreso se actualizan automáticamente
- [ ] ✅ Todas las secciones cargan datos correctamente
- [ ] ✅ Modales se abren y cierran correctamente
- [ ] ✅ Responsive design funciona en móvil

---

## 🎮 **¡Disfruta probando tu sistema de gamificación!**

Esta aplicación te permite probar de forma visual e interactiva todas las funcionalidades de tu API de Booky, con especial énfasis en el sistema de gamificación que hemos implementado.
