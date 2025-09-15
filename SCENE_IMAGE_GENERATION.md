# Scene Image Generation Feature

Este módulo implementa la funcionalidad de generación de imágenes 360° para escenas de libros usando OpenAI (GPT + DALL-E).

## Funcionalidad

### Endpoint Principal
`POST /api/books/{bookId}/scene-image`

Genera una imagen 360° equirectangular basada en un fragmento de texto del libro especificado.

### Request Body
```json
{
  "text": "fragmento narrado del libro",
  "style": "opcional: realista / ilustración / óleo / lowpoly ...",
  "seed": 42,
  "return_base64": false,
  "size": "4096x2048"
}
```

### Response
```json
{
  "book_id": "123",
  "crafted_prompt": "prompt final usado en imágenes",
  "image_url": "https://.../image.png",
  "image_base64": null,
  "size": "4096x2048",
  "style": "photorealistic",
  "seed": 42,
  "created_at": "2025-09-15T22:10:00Z"
}
```

## Configuración

### Variables de Entorno Requeridas
```bash
# OpenAI Configuration
OPENAI_API_KEY=your-openai-api-key
OPENAI_CHAT_MODEL=gpt-4o  # opcional, default: gpt-4o
OPENAI_IMAGE_MODEL=dall-e-3  # opcional, default: dall-e-3

# Scene Image Configuration
SCENE_IMAGE_DEFAULT_SIZE=4096x2048  # opcional
SCENE_IMAGE_MAX_TEXT_LENGTH=2000  # opcional
SCENE_IMAGE_MIN_TEXT_LENGTH=15  # opcional
SCENE_IMAGE_RATE_LIMIT=10  # requests per minute, opcional
```

### application.yml
Las configuraciones se encuentran en `application.yml`:

```yaml
openai:
  api-key: ${OPENAI_API_KEY:}
  chat-model: ${OPENAI_CHAT_MODEL:gpt-4o}
  image-model: ${OPENAI_IMAGE_MODEL:dall-e-3}
  base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
  timeout: ${OPENAI_TIMEOUT:30s}
  max-retries: ${OPENAI_MAX_RETRIES:3}

scene-image:
  default-size: ${SCENE_IMAGE_DEFAULT_SIZE:4096x2048}
  max-text-length: ${SCENE_IMAGE_MAX_TEXT_LENGTH:2000}
  min-text-length: ${SCENE_IMAGE_MIN_TEXT_LENGTH:15}
  rate-limit:
    requests-per-minute: ${SCENE_IMAGE_RATE_LIMIT:10}
```

## Ejemplo de Uso

### cURL
```bash
curl -X POST "http://localhost:8080/api/books/123/scene-image" \
  -H "Content-Type: application/json" \
  -d '{
    "text": "El amanecer tiñe de naranja la biblioteca circular; estanterías de roble llegan al techo, polvo en suspensión, vitrales proyectan figuras geométricas sobre el suelo de mármol.",
    "style": "photorealistic",
    "seed": 42,
    "return_base64": false,
    "size": "4096x2048"
  }'
```

### JavaScript/Fetch
```javascript
const response = await fetch('/api/books/123/scene-image', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    text: "El amanecer tiñe de naranja la biblioteca circular...",
    style: "photorealistic",
    size: "4096x2048",
    return_base64: false
  })
});

const result = await response.json();
console.log('Generated image URL:', result.image_url);
```

## Endpoints Adicionales

### Obtener generaciones de un libro
```bash
GET /api/books/{bookId}/scene-generations
```

### Obtener contador de generaciones
```bash
GET /api/books/{bookId}/scene-generations/count
```

## Validaciones

- **Texto**: Entre 15 y 2000 caracteres
- **Tamaño**: Solo formatos 2:1 para imágenes 360° (1024x512, 2048x1024, 4096x2048)
- **Rate Limiting**: 10 requests por minuto por IP
- **Libro**: Debe existir en la base de datos

## Códigos de Respuesta

- `200` - Imagen generada exitosamente
- `400` - Parámetros de request inválidos
- `404` - Libro no encontrado
- `422` - Formato de tamaño de imagen inválido
- `429` - Rate limit excedido
- `503` - Servicio OpenAI no disponible

## Arquitectura

### Componentes Principales

1. **SceneImageController** - REST endpoint
2. **SceneImageService** - Lógica de negocio principal
3. **PromptCraftService** - Generación de prompts usando GPT
4. **OpenAIClient** - Cliente para APIs de OpenAI
5. **SceneImageGenerationRepository** - Persistencia de auditoría

### Base de Datos

La tabla `scene_image_generations` almacena:
- Metadatos de la generación
- Hash del fragmento (para cache)
- Prompt final utilizado
- URL y/o base64 de la imagen
- Métricas de rendimiento y costo

## Tests

### Ejecutar Tests
```bash
mvn test
```

### Coverage
- **Unit Tests**: SceneImageService, PromptCraftService
- **Integration Tests**: SceneImageController
- **Mocks**: OpenAI client, repositorios

## Seguridad

- API key de OpenAI por variable de entorno
- Rate limiting por IP
- Sanitización de inputs
- Logs sin información sensible

## Monitoreo

### Métricas Registradas
- Tiempo de respuesta de OpenAI
- Tokens utilizados
- Costo estimado
- Errores por tipo

### Logs
- Generaciones exitosas
- Errores de servicio
- Rate limiting aplicado

## Troubleshooting

### Error 503 - OpenAI Service Unavailable
- Verificar OPENAI_API_KEY
- Comprobar conectividad a api.openai.com
- Revisar límites de rate de OpenAI

### Error 422 - Invalid Image Size
- Solo se soportan tamaños 2:1 para imágenes 360°
- Tamaños válidos: 1024x512, 2048x1024, 4096x2048

### Imagen generada no es 360°
- Verificar que el prompt incluye "equirectangular 2:1"
- Revisar el prompt generado en la respuesta

## Configuración de Desarrollo

Para desarrollo local sin OpenAI:
1. Usar mocks en tests
2. Configurar fallback prompts
3. Usar imágenes de prueba estáticas

---

## Estructura de Archivos Creados

```
src/main/java/com/uade/bookybe/
├── config/
│   ├── OpenAIConfig.java
│   ├── SceneImageConfig.java
│   └── WebClientConfig.java
├── core/
│   ├── exception/
│   │   ├── BookNotFoundException.java
│   │   ├── InvalidImageSizeException.java
│   │   └── OpenAIServiceException.java
│   ├── model/
│   │   ├── SceneImageGeneration.java (JPA Entity)
│   │   └── dto/
│   │       ├── ImageResult.java
│   │       ├── SceneImageRequest.java
│   │       └── SceneImageResponse.java
│   ├── port/
│   │   └── SceneImageGenerationRepository.java
│   └── service/
│       ├── PromptCraftService.java
│       ├── SceneImageService.java
│       └── gateway/
│           └── OpenAIClient.java
└── router/controller/
    └── SceneImageController.java

src/test/java/com/uade/bookybe/
├── core/service/
│   ├── PromptCraftServiceTest.java
│   └── SceneImageServiceTest.java
└── router/controller/
    └── SceneImageControllerTest.java
```
