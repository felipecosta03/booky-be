# 📚 Scripts de Alta de Usuarios - Booky Backend

Este directorio contiene scripts para dar de alta usuarios en el sistema Booky de diferentes maneras.

## 🛠️ Scripts Disponibles

### 1. Script Bash Interactive (`create_user.sh`)

Script interactivo que utiliza la API REST para crear usuarios.

#### Características:
- ✅ Interfaz interactiva amigable
- ✅ Validación de emails
- ✅ Verificación de contraseñas
- ✅ Carga masiva desde archivo CSV
- ✅ Usuarios de prueba predefinidos
- ✅ Colores en la terminal
- ✅ Manejo de errores

#### Uso:

```bash
# Dar permisos de ejecución
chmod +x create_user.sh

# Ejecutar con servidor local
./create_user.sh

# Ejecutar con servidor remoto
./create_user.sh https://api.booky.com

# Ver ayuda
./create_user.sh --help
```

#### Opciones del menú:
1. **Crear usuario interactivo**: Pregunta datos paso a paso
2. **Crear usuarios desde archivo CSV**: Carga masiva desde archivo
3. **Crear usuarios de prueba**: Crea 5 usuarios predefinidos
4. **Cambiar URL de API**: Configurar servidor remoto
5. **Ayuda**: Mostrar información de uso

### 2. Archivo CSV de Ejemplo (`usuarios_ejemplo.csv`)

Archivo de ejemplo para carga masiva de usuarios.

#### Formato:
```csv
nombre,apellido,email,username,password
Roberto,Silva,roberto.silva@empresa.com,robertos,MiPassword123
Fernanda,Lopez,fernanda.lopez@empresa.com,ferlopez,Password456
```

#### Uso:
1. Editar el archivo con tus usuarios
2. Usar opción 2 del script bash
3. Proporcionar la ruta del archivo

### 3. Script SQL (`alta_usuarios.sql`)

Script SQL para inserción masiva directa en la base de datos.

#### Características:
- ✅ 16 usuarios de prueba (2 admin + 14 regulares)
- ✅ 16 direcciones en diferentes países
- ✅ Contraseñas encriptadas con BCrypt
- ✅ Datos realistas y variados
- ✅ Consultas de verificación incluidas

#### Uso:

```bash
# Conectar a PostgreSQL y ejecutar
psql -h localhost -U usuario -d booky_db -f alta_usuarios.sql

# O usando pgAdmin, DBeaver, etc.
```

#### Usuarios creados:
- **Administradores**: `admin`, `superadmin`
- **Usuarios regulares**: 14 usuarios con diferentes perfiles
- **Contraseña universal**: `password123` (encriptada)

## 🚀 Guía de Inicio Rápido

### Opción 1: Usar API REST (Recomendado)

```bash
# 1. Asegúrate de que el servidor esté corriendo
mvn spring-boot:run

# 2. Ejecutar el script
./scripts/create_user.sh

# 3. Seleccionar opción 3 para crear usuarios de prueba
```

### Opción 2: Usar Script SQL

```bash
# 1. Conectar a la base de datos
psql -h localhost -U tu_usuario -d booky_db

# 2. Ejecutar el script
\i scripts/alta_usuarios.sql
```

### Opción 3: Carga Masiva desde CSV

```bash
# 1. Editar el archivo CSV
nano scripts/usuarios_ejemplo.csv

# 2. Ejecutar el script
./scripts/create_user.sh

# 3. Seleccionar opción 2 y proporcionar la ruta del CSV
```

## 📋 Requisitos

### Para Script Bash:
- ✅ `curl` instalado
- ✅ Servidor Booky ejecutándose
- ✅ Bash 4.0 o superior
- ✅ Permisos de ejecución

### Para Script SQL:
- ✅ PostgreSQL client (`psql`)
- ✅ Acceso a la base de datos
- ✅ Permisos de INSERT en tablas `users` y `addresses`

## 🔧 Configuración

### Variables de Entorno (Opcional)

```bash
# Configurar URL por defecto
export BOOKY_API_URL="https://api.booky.com"

# Usar en el script
./create_user.sh $BOOKY_API_URL
```

### Personalización de Contraseñas

Para generar nuevos hashes BCrypt:

1. Visitar: https://bcrypt-generator.com/
2. Configurar **Cost Factor: 10**
3. Ingresar la contraseña
4. Copiar el hash generado

## 🐛 Solución de Problemas

### Error: "curl: command not found"
```bash
# Ubuntu/Debian
sudo apt-get install curl

# macOS
brew install curl

# CentOS/RHEL
sudo yum install curl
```

### Error: "Connection refused"
- ✅ Verificar que el servidor esté ejecutándose en el puerto correcto
- ✅ Revisar la URL de la API
- ✅ Verificar firewall/proxy

### Error: "Duplicate key value"
- ✅ El usuario ya existe en la base de datos
- ✅ Cambiar email o username
- ✅ Limpiar la base de datos si es entorno de prueba

### Error: "Bad Request (400)"
- ✅ Verificar formato de los datos
- ✅ Asegurar que la contraseña no esté vacía
- ✅ Validar formato del email

## 📝 Ejemplos de Uso

### Crear Usuario Individual via API

```bash
curl -X POST http://localhost:8080/sign-up \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test",
    "lastname": "User", 
    "email": "test@example.com",
    "username": "testuser",
    "password": "mypassword123"
  }'
```

### Verificar Usuarios Creados

```sql
-- Ver todos los usuarios
SELECT username, name, lastname, email, coins 
FROM users 
ORDER BY date_created DESC;

-- Contar usuarios por país
SELECT a.country, COUNT(*) as total
FROM users u
JOIN addresses a ON u.address_id = a.id
GROUP BY a.country;
```

## 🔐 Seguridad

- ✅ Todas las contraseñas se envían encriptadas
- ✅ Validación de formato de email
- ✅ Verificación de duplicados
- ✅ Manejo seguro de errores
- ✅ No se muestran contraseñas en logs

## 📞 Soporte

Si tienes problemas con los scripts:

1. Verificar que el servidor esté corriendo
2. Revisar los logs del servidor
3. Probar con un usuario simple primero
4. Verificar permisos de archivos

## 🔄 Actualización

Para actualizar los scripts:

```bash
# Hacer backup de datos personalizados
cp usuarios_ejemplo.csv usuarios_ejemplo.csv.bak

# Descargar nuevas versiones
# ... actualizar archivos ...

# Restaurar datos personalizados si es necesario
``` 