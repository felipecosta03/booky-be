# 🔒 Seguridad del Proyecto

## ⚠️ Archivos que NUNCA deben subirse a GitHub

Los siguientes archivos contienen credenciales y datos sensibles y están **automáticamente excluidos** del repositorio a través de `.gitignore`:

### 📄 Archivos de Configuración con Credenciales
- `github-secrets.txt`
- `github-secrets-sandbox.txt` 
- `.env`
- `.env.local`
- `.env.prod`
- `.env.production`

### 🔑 Archivos de Llaves y Certificados
- `*.pem` (llaves privadas SSH)
- `*.key` (cualquier archivo de llave)
- `booky-key.pem` (llave SSH para EC2)

### 🗂️ Configuración AWS Local
- `.aws/` (carpeta de configuración AWS)
- `aws-credentials.txt`

### 💾 Backups que pueden contener datos sensibles
- `backups/`
- `*.sql`
- `*.dump`

## ✅ Protección Automática

El archivo `.gitignore` ya está configurado para proteger estos archivos automáticamente. **NO tienes que hacer nada especial** - simplemente estos archivos no se subirán.

## 🛡️ Qué hacer si accidentalmente subes credenciales

### Si subiste credenciales a GitHub:

1. **Inmediatamente** cambia las credenciales en AWS
2. **Revoca** las llaves en AWS Console
3. **Genera nuevas credenciales**
4. **Actualiza** los GitHub Secrets
5. **Ejecuta** el script de configuración nuevamente

### Para limpiar el historial:
```bash
# Eliminar archivo del historial de git
git rm --cached archivo-con-credenciales.txt
git commit -m "Remove sensitive file"
git push origin main
```

## 🔍 Verificación de Seguridad

### Antes de hacer commit, verifica:
```bash
# Ver qué archivos se van a subir
git status

# Verificar que no hay archivos sensibles
git diff --cached

# Verificar .gitignore
cat .gitignore
```

### Comandos útiles:
```bash
# Ver archivos ignorados
git status --ignored

# Verificar qué archivos están tracked
git ls-files
```

## 🎯 Mejores Prácticas

### ✅ Hacer
- Usa los scripts de configuración proporcionados
- Revisa `.gitignore` antes de hacer commits
- Mantén las credenciales solo en GitHub Secrets
- Usa variables de entorno para credenciales

### ❌ No hacer
- Hardcodear credenciales en el código
- Subir archivos `.env` o `.pem`
- Compartir credenciales por email o chat
- Usar credenciales en logs o comentarios

## 🔐 Configuración de GitHub Secrets

### Secrets requeridos:
- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `AWS_SESSION_TOKEN` (solo para sandbox)
- `AWS_REGION`
- `DATABASE_PASSWORD`
- `JWT_SECRET`
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

### Cómo configurar:
1. Repository → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Añade name y value
4. Click "Add secret"

## 🚨 Alertas de Seguridad

GitHub puede detectar credenciales expuestas y enviarte alertas. Si recibes una:

1. **Actúa inmediatamente**
2. **Revoca las credenciales**
3. **Genera nuevas**
4. **Actualiza los secrets**

## 📞 Contacto

Si tienes dudas sobre seguridad o crees que hay una vulnerabilidad, es importante actuar rápidamente:

1. Revoca las credenciales comprometidas
2. Genera nuevas credenciales
3. Actualiza la configuración
4. Ejecuta el script de configuración nuevamente

## 🎓 Educación

### Recursos útiles:
- [GitHub Security Best Practices](https://docs.github.com/en/code-security)
- [AWS Security Best Practices](https://aws.amazon.com/security/)
- [Spring Boot Security](https://spring.io/projects/spring-security)

---

**🔐 Recuerda: La seguridad es responsabilidad de todos. Mantén tus credenciales seguras.** 