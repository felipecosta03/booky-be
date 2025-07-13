# Security Configuration Guide

## Overview

The application now uses **conditional properties** instead of Spring profiles for security configuration. This provides more flexibility and cleaner configuration management.

## Configuration Options

### Security Property

The main security control is managed through the `app.security.enabled` property:

```yaml
app:
  security:
    enabled: true/false
```

### Environment Variable

You can also control security through the `SECURITY_ENABLED` environment variable:

```bash
SECURITY_ENABLED=true  # Enable security
SECURITY_ENABLED=false # Disable security
```

## Environments

### Development (Local)

- **Profile**: `local`
- **Security**: `DISABLED` by default
- **Configuration**: `application.yml`
- **Access**: All endpoints are open, no authentication required

```yaml
app:
  security:
    enabled: false # No security for development
```

### Production (AWS)

- **Profile**: `prod`
- **Security**: `ENABLED` by default
- **Configuration**: `application-prod.yml`
- **Access**: Protected endpoints require authentication

```yaml
app:
  security:
    enabled: true # Security enabled in production
```

## Docker Compose Configurations

### Local Development (`docker-compose.yml`)

```yaml
environment:
  SPRING_PROFILES_ACTIVE: local
  SECURITY_ENABLED: false
  JWT_SECRET: booky-development-secret-key-for-development-only-32-chars
```

### Production (`docker-compose-ec2.yml`)

```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
  SECURITY_ENABLED: true
  JWT_SECRET: ${JWT_SECRET:-booky-production-secret-key-change-me-in-production}
```

## Implementation Details

### SecurityConfig.java

The security configuration uses `@ConditionalOnProperty` annotations:

```java
@ConditionalOnProperty(
    name = "app.security.enabled",
    havingValue = "true",
    matchIfMissing = false)
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    // Security enabled configuration
}

@ConditionalOnProperty(
    name = "app.security.enabled", 
    havingValue = "false",
    matchIfMissing = true)
@Bean
public SecurityFilterChain noSecurityFilterChain(HttpSecurity http) throws Exception {
    // Security disabled configuration
}
```

### Protected Endpoints (when security is enabled)

- **Public endpoints**: `/sign-up`, `/sign-in`, `/actuator/**`, `/swagger-ui/**`, `/v3/api-docs/**`
- **Protected endpoints**: All other endpoints require authentication

## Environment Variables

### Required for Production

```bash
# Security
SECURITY_ENABLED=true
JWT_SECRET=your-super-secret-jwt-key-minimum-32-characters

# Database
DATABASE_URL=jdbc:postgresql://postgres:5432/booky
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your-secure-password

# Cloudinary (optional)
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

## Testing

### Test Security is Disabled (Development)

```bash
# Should return 200 without authentication
curl http://localhost:8080/users/user-001/library
```

### Test Security is Enabled (Production)

```bash
# Should return 401 or 403 without authentication
curl http://52.15.181.167:8080/users/user-001/library

# Should work with valid JWT token
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://52.15.181.167:8080/users/user-001/library
```

## Migration Benefits

1. **Flexibility**: Can enable/disable security without changing profiles
2. **Environment Control**: Override security settings via environment variables
3. **Cleaner Code**: No need for multiple profile-specific configurations
4. **Better Testing**: Easier to test different security scenarios
5. **Production Ready**: Clear separation between development and production security

## Troubleshooting

### Common Issues

1. **Security not working**: Check `SECURITY_ENABLED` environment variable
2. **JWT errors**: Verify `JWT_SECRET` is set and has minimum 32 characters
3. **Profile issues**: Ensure `SPRING_PROFILES_ACTIVE` is set correctly
4. **Database connection**: Verify database environment variables

### Debug Commands

```bash
# Check environment variables
docker exec booky-backend env | grep -E "(SECURITY|JWT|SPRING_PROFILES)"

# Check application properties
docker exec booky-backend cat /app/application.yml

# Check logs
docker logs booky-backend | grep -i security
``` 