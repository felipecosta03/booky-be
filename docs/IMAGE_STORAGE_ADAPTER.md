# Image Storage Adapter - Documentación

## 🏗️ Arquitectura

El sistema de almacenamiento de imágenes utiliza el **patrón Adapter** siguiendo la arquitectura hexagonal:

```
Core (Domain) ← Port (Interface) ← Adapter (Implementation)
```

### Componentes:

- **`ImageStoragePort`** (Core): Interface que define las operaciones de almacenamiento
- **`CloudinaryImageStorageAdapter`** (Infrastructure): Implementación específica para Cloudinary
- **`UserServiceImpl`** (Core): Usa el port sin conocer la implementación específica

## ⚙️ Configuración

### 1. Credenciales de Cloudinary

Crear cuenta en [Cloudinary](https://cloudinary.com/) y obtener:
- Cloud name
- API Key  
- API Secret

### 2. Configurar application.yml

```yaml
cloudinary:
  cloud-name: tu-cloud-name
  api-key: tu-api-key
  api-secret: tu-api-secret
```

### 3. Variables de entorno (Recomendado para producción)

```bash
CLOUDINARY_CLOUD_NAME=tu-cloud-name
CLOUDINARY_API_KEY=tu-api-key
CLOUDINARY_API_SECRET=tu-api-secret
```

## 🔧 Funcionalidades

### Subida de Imágenes
```java
Optional<String> imageUrl = imageStoragePort.uploadImage(file, "booky/users");
```

**Características:**
- ✅ Conversión automática a WebP para optimización
- ✅ Compresión inteligente (`quality: auto:good`)
- ✅ Nombres únicos para evitar colisiones
- ✅ Organización por carpetas

### Eliminación de Imágenes
```java
boolean deleted = imageStoragePort.deleteImage(imageUrl);
```

### URLs Optimizadas
```java
String optimizedUrl = imageStoragePort.getOptimizedUrl(imageUrl, 300, 300);
```

## 🎯 Endpoints que Usan el Adapter

### PUT `/users` - Actualizar Usuario con Imagen
```bash
curl -X PUT "http://localhost:8080/users" \
  -F 'user={"id":"123","name":"Juan","lastname":"Pérez"};type=application/json' \
  -F 'image=@/path/to/image.jpg;type=image/jpeg'
```

**Flujo:**
1. Controller recibe `MultipartFile` + `UserUpdateDto`
2. Mapea DTO → Model
3. Service usa `ImageStoragePort.uploadImage()`
4. Cloudinary procesa y retorna URL
5. URL se guarda en base de datos

## 🔄 Migración a S3 (Futuro)

### 1. Crear nuevo adapter
```java
@Component
public class S3ImageStorageAdapter implements ImageStoragePort {
    // Implementación específica para S3
}
```

### 2. Configurar cuál adapter usar
```java
@Configuration
public class ImageStorageConfig {
    
    @Bean
    @ConditionalOnProperty(name = "image.storage.provider", havingValue = "s3")
    public ImageStoragePort s3ImageStoragePort() {
        return new S3ImageStorageAdapter();
    }
    
    @Bean
    @ConditionalOnProperty(name = "image.storage.provider", havingValue = "cloudinary", matchIfMissing = true)
    public ImageStoragePort cloudinaryImageStoragePort() {
        return new CloudinaryImageStorageAdapter();
    }
}
```

### 3. Actualizar configuración
```yaml
image:
  storage:
    provider: s3  # cambiar de cloudinary a s3
```

## 🛠️ Testing

### Test del Adapter
```java
@SpringBootTest
class CloudinaryImageStorageAdapterTest {
    
    @Autowired
    private ImageStoragePort imageStoragePort;
    
    @Test
    void shouldUploadImage() {
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        Optional<String> result = imageStoragePort.uploadImage(file, "test");
        assertTrue(result.isPresent());
    }
}
```

### Mock para Testing
```java
@TestConfiguration
public class TestConfig {
    
    @Bean
    @Primary
    public ImageStoragePort mockImageStoragePort() {
        return Mockito.mock(ImageStoragePort.class);
    }
}
```

## 🚀 Ventajas de esta Arquitectura

1. **Intercambiabilidad**: Fácil cambio entre proveedores (Cloudinary ↔ S3)
2. **Testabilidad**: El service no depende de implementaciones externas
3. **Separación de responsabilidades**: Core sin dependencias de infraestructura
4. **Mantenibilidad**: Cambios en el proveedor no afectan la lógica de negocio
5. **Escalabilidad**: Agregar nuevos proveedores sin modificar código existente

## 📝 Logs

El adapter genera logs detallados:

```
INFO  - Uploading image to Cloudinary. File: avatar.jpg, Folder: booky/users
INFO  - Image uploaded successfully. URL: https://res.cloudinary.com/...
INFO  - Deleting image from Cloudinary. Public ID: booky/users/image123
WARN  - Could not extract public_id from URL: invalid-url
ERROR - Error uploading image to Cloudinary: Connection timeout
```

## 🔍 Troubleshooting

### Error: "Credenciales inválidas"
- Verificar cloud-name, api-key y api-secret
- Revisar que las variables de entorno estén configuradas

### Error: "Archivo muy grande"
- Cloudinary tiene límites de tamaño según el plan
- Considerar redimensionar antes de subir

### Error: "URL no encontrada para eliminación"
- Verificar que la URL sea de Cloudinary
- Revisar logs para ver el public_id extraído 