# 🎮 Sistema de Gamificación - Booky

## 📋 Resumen

Se ha implementado un sistema de gamificación completo para la aplicación Booky que incluye:

- **Sistema de Puntos**: Otorga puntos por diferentes actividades
- **Logros (Achievements)**: Reconocimientos por completar objetivos específicos  
- **Niveles de Usuario**: Progresión basada en puntos acumulados
- **Integración Cross-Funcional**: Eventos de gamificación en todas las funcionalidades

## 🏗️ Arquitectura Implementada

### Modelos de Dominio

#### `GamificationProfile`
- Perfil principal de gamificación del usuario
- Contiene puntos totales, nivel actual, estadísticas de actividades
- Relación 1:1 con `User`

#### `Achievement`
- Definición de logros disponibles
- Incluye nombre, descripción, categoría, condición y recompensa en puntos
- Configurables a través de base de datos

#### `UserLevel`
- Definición de niveles (1-7: Novato a Leyenda)
- Rangos de puntos, nombres, descripciones y badges

#### `UserAchievement`
- Relación entre usuario y logros obtenidos
- Incluye fecha de obtención y estado de notificación

### Entidades JPA

Se crearon las siguientes entidades de base de datos:

- `GamificationProfileEntity`
- `AchievementEntity` 
- `UserLevelEntity`
- `UserAchievementEntity`

Con sus respectivos repositorios y mappers usando MapStruct.

## 🎯 Sistema de Puntos

### Enum de Actividades de Gamificación

El sistema ahora utiliza un **enum type-safe** (`GamificationActivity`) que define todas las actividades y sus puntos:

```java
public enum GamificationActivity {
  // Book-related activities  
  BOOK_ADDED(10, "Agregar libro a biblioteca"),
  BOOK_READ(25, "Marcar libro como leído"),
  BOOK_FAVORITED(5, "Marcar libro como favorito"),
  BOOK_OFFERED_FOR_EXCHANGE(15, "Ofrecer libro para intercambio"),
  
  // Exchange-related activities
  EXCHANGE_CREATED(20, "Crear solicitud de intercambio"),
  EXCHANGE_COMPLETED(50, "Completar intercambio exitoso"),
  
  // Social activities
  POST_CREATED(15, "Crear post"),
  COMMENT_CREATED(10, "Crear comentario"),
  
  // Community activities
  COMMUNITY_JOINED(20, "Unirse a comunidad"),
  COMMUNITY_CREATED(100, "Crear comunidad"),
  
  // Reading club activities
  READING_CLUB_JOINED(25, "Unirse a club de lectura"),
  READING_CLUB_CREATED(75, "Crear club de lectura");
}
```

### Ventajas del Enum:
- ✅ **Type Safety**: No más strings mágicos
- ✅ **Centralizado**: Puntos y descripciones en un solo lugar
- ✅ **Mantenible**: Fácil agregar/modificar actividades
- ✅ **Documentado**: Cada actividad tiene su descripción
- ✅ **Refactoring Safe**: IDE puede refactorizar automáticamente

### Uso en el Código:
```java
// Antes (con Map y strings)
awardPoints(userId, "BOOK_READ", POINT_VALUES.get("BOOK_READ"));

// Ahora (con enum type-safe)
awardPoints(userId, GamificationActivity.BOOK_READ);
```

## 🏆 Sistema de Logros

### Categorías de Logros

#### 📚 Lector
- **Primer Libro**: Agregar primer libro (+25 pts)
- **Lector Novato**: Leer 5 libros (+50 pts)
- **Lector Experimentado**: Leer 25 libros (+100 pts)
- **Maestro Lector**: Leer 100 libros (+250 pts)
- **Coleccionista**: Alcanzar 250 puntos (+50 pts)

#### 🤝 Intercambios
- **Primer Intercambio**: Completar primer intercambio (+75 pts)
- **Intercambiador**: Completar 10 intercambios (+150 pts)
- **Maestro del Intercambio**: Completar 50 intercambios (+300 pts)

#### 👥 Social
- **Primera Palabra**: Escribir primer post (+25 pts)
- **Conversador**: Hacer 50 comentarios (+100 pts)
- **Sociable**: Unirse a 5 comunidades (+75 pts)
- **Líder**: Crear una comunidad (+200 pts)
- **Blogger**: Escribir 100 posts (+250 pts)

#### 📖 Clubes
- **Nuevo Miembro**: Unirse a primer club (+50 pts)
- **Club Master**: Crear un club (+150 pts)

#### 🎯 Hitos
- **Centurión**: 100 puntos (+25 pts)
- **Medio Millar**: 500 puntos (+75 pts)
- **Milenario**: 1000 puntos (+150 pts)
- **Bi-Milenario**: 2000 puntos (+300 pts)

## 📈 Sistema de Niveles

| Nivel | Nombre | Rango de Puntos | Badge |
|-------|--------|-----------------|-------|
| 1 | Novato | 0-99 | 🌱 |
| 2 | Aprendiz | 100-249 | 📚 |
| 3 | Lector | 250-499 | 🤓 |
| 4 | Bibliófilo | 500-999 | 📖 |
| 5 | Experto | 1000-1999 | 🎓 |
| 6 | Maestro | 2000-3999 | 👑 |
| 7 | Leyenda | 4000+ | ⭐ |

## 🔌 Integración con Servicios Existentes

### Servicios Modificados

#### `BookServiceImpl`
- `addBookToUserLibrary()`: Otorga puntos por agregar libro
- `updateBookStatus()`: Otorga puntos cuando se marca como leído
- `updateBookExchangePreference()`: Otorga puntos por ofrecer intercambio
- `toggleBookFavorite()`: Otorga puntos por marcar favorito

#### `PostServiceImpl`
- `createPost()`: Otorga puntos por crear post

#### `CommentServiceImpl`
- `createComment()`: Otorga puntos por crear comentario

#### `BookExchangeServiceImpl`
- `createExchange()`: Otorga puntos por crear intercambio
- `updateExchangeStatus()`: Otorga puntos cuando se completa (a ambos usuarios)

## 🌐 API Endpoints

### Controlador de Gamificación

#### Perfil del Usuario
- `GET /gamification/profile/{userId}` - Obtener perfil completo (auto-inicializa si no existe)
- `POST /gamification/profile/{userId}/initialize` - Inicializar perfil manualmente (uso interno)

#### Logros
- `GET /gamification/achievements` - Todos los logros disponibles
- `GET /gamification/achievements/{userId}` - Logros del usuario
- `GET /gamification/achievements/{userId}/unnotified` - Logros no notificados
- `PUT /gamification/achievements/{userId}/mark-notified` - Marcar como notificados
- `POST /gamification/achievements/{userId}/check` - Verificar nuevos logros

#### Niveles
- `GET /gamification/levels` - Todos los niveles disponibles

#### Actividades
- `GET /gamification/activities` - Todas las actividades y sus puntos

## 🗄️ Scripts de Base de Datos

### Schema (`gamification_schema.sql`)
Crea las siguientes tablas:
- `user_levels`
- `achievements` 
- `gamification_profiles`
- `user_achievements`

### Datos Iniciales (`gamification_data.sql`)
Inserta:
- 7 niveles de usuario
- 19 logros predefinidos en 5 categorías

## 🚀 Instalación y Configuración

### 1. Ejecutar Scripts SQL
```sql
-- Aplicar esquema
\i src/main/resources/sql/gamification_schema.sql

-- Cargar datos iniciales  
\i src/main/resources/sql/gamification_data.sql

-- (Opcional) Migrar FK si ya tenías tablas creadas
\i src/main/resources/sql/migrate_gamification_fk.sql
```

### 🔧 Solución a Problemas de FK

Si tienes problemas al eliminar usuarios debido a foreign keys de gamificación:

1. **Verificar FK actuales**:
```sql
\i src/main/resources/sql/check_gamification_fk.sql
```

2. **Aplicar migración de FK**:
```sql
\i src/main/resources/sql/migrate_gamification_fk.sql
```

3. **Resultado esperado**: Todas las FK deben tener `delete_rule = 'CASCADE'`

### 2. Verificar Integración
El sistema está completamente integrado y funcionará automáticamente cuando:
- Se agreguen libros a bibliotecas
- Se creen posts y comentarios
- Se procesen intercambios
- Se unan usuarios a comunidades

### 3. Inicialización Automática
Los perfiles de gamificación se inicializan **automáticamente**:
- ✅ **Al registrarse**: Nuevo usuario → perfil creado automáticamente
- ✅ **Primera acción**: Usuario existente sin perfil → auto-inicialización
- ⚙️ **Manual**: `POST /gamification/profile/{userId}/initialize` (solo para casos especiales)

## 🔄 Flujo de Funcionamiento

1. **Usuario realiza acción** (ej: agregar libro)
2. **Servicio procesa acción** normalmente
3. **Evento de gamificación** se dispara automáticamente
4. **GamificationService** otorga puntos y actualiza estadísticas
5. **Sistema verifica logros** automáticamente
6. **Niveles se actualizan** si es necesario
7. **Nuevos logros se crean** si se cumplen condiciones

## 📊 Características Técnicas

- **Type Safe**: Uso de enums para prevenir errores de programación
- **Transaccional**: Todas las operaciones respetan las transacciones
- **Asíncrono**: Los eventos de gamificación no bloquean operaciones principales
- **Escalable**: Arquitectura preparada para millones de usuarios
- **Configurable**: Logros y niveles configurables vía base de datos
- **Extensible**: Fácil agregar nuevos tipos de logros y actividades
- **Mantenible**: Código limpio sin strings mágicos ni constantes dispersas

## 🎯 Próximos Pasos Recomendados

1. **Notificaciones Push**: Integrar con sistema de notificaciones
2. **Badges Visuales**: Crear sistema de badges visuales en frontend
3. **Eventos Temporales**: Logros por tiempo limitado o estacionales
4. **Gamificación Social**: Logros basados en interacciones entre usuarios
5. **Métricas y Analytics**: Dashboard para análisis de engagement

---

¡El sistema está completamente funcional y listo para usar! 🎉
