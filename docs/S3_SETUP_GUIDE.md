# S3 Setup Guide para Booky Backend

## Configuración de AWS S3 para Almacenamiento de Imágenes

### 1. Crear el Bucket S3

```bash
# Crear el bucket (reemplaza 'us-east-1' con tu región preferida)
aws s3 mb s3://bucket-user-images-store --region us-east-1

# Verificar que el bucket se creó correctamente
aws s3 ls s3://bucket-user-images-store
```

### 2. Configurar la Policy del Bucket (Opcional)

Si quieres que las imágenes sean públicamente accesibles (no recomendado para producción):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "PublicReadGetObject",
      "Effect": "Allow",
      "Principal": "*",
      "Action": "s3:GetObject",
      "Resource": "arn:aws:s3:::bucket-user-images-store/*"
    }
  ]
}
```

**Aplicar la policy del bucket:**
```bash
aws s3api put-bucket-policy --bucket bucket-user-images-store --policy file://bucket-policy.json
```

### 3. Crear Usuario IAM y Aplicar Policy

#### Paso 1: Crear el usuario IAM
```bash
# Crear usuario IAM
aws iam create-user --user-name booky-s3-user

# Crear access key para el usuario
aws iam create-access-key --user-name booky-s3-user
```

#### Paso 2: Aplicar la policy IAM
```bash
# Crear la policy desde el archivo JSON
aws iam create-policy --policy-name BookyS3ImagePolicy --policy-document file://docs/S3_IAM_POLICY.json

# Obtener el ARN de la policy (reemplaza ACCOUNT-ID con tu ID de cuenta)
POLICY_ARN="arn:aws:iam::ACCOUNT-ID:policy/BookyS3ImagePolicy"

# Asociar la policy al usuario
aws iam attach-user-policy --user-name booky-s3-user --policy-arn $POLICY_ARN
```

### 4. Configurar las Variables de Entorno

#### Para Development (docker-compose.yml):
```yaml
environment:
  IMAGE_STORAGE_STRATEGY: s3
  AWS_S3_ACCESS_KEY: tu-access-key
  AWS_S3_SECRET_KEY: tu-secret-key
  AWS_S3_REGION: us-east-1
  AWS_S3_BUCKET: bucket-user-images-store
```

#### Para Production (usando IAM Roles - recomendado):
```yaml
environment:
  IMAGE_STORAGE_STRATEGY: s3
  AWS_S3_REGION: us-east-1
  AWS_S3_BUCKET: bucket-user-images-store
  # No incluir AWS_S3_ACCESS_KEY ni AWS_S3_SECRET_KEY
  # La aplicación usará el IAM Role de la instancia EC2
```

### 5. Configurar CORS (Cross-Origin Resource Sharing)

Si planeas acceder a las imágenes desde el frontend, configura CORS:

```json
[
  {
    "AllowedHeaders": ["*"],
    "AllowedMethods": ["GET", "PUT", "POST", "DELETE"],
    "AllowedOrigins": ["*"],
    "ExposeHeaders": []
  }
]
```

**Aplicar configuración CORS:**
```bash
aws s3api put-bucket-cors --bucket bucket-user-images-store --cors-configuration file://cors-config.json
```

### 6. Configurar Lifecycle Policy (Opcional)

Para manejar imágenes temporales o limpiar cargas fallidas:

```json
{
  "Rules": [
    {
      "ID": "DeleteIncompleteMultipartUploads",
      "Status": "Enabled",
      "AbortIncompleteMultipartUpload": {
        "DaysAfterInitiation": 1
      }
    }
  ]
}
```

**Aplicar lifecycle policy:**
```bash
aws s3api put-bucket-lifecycle-configuration --bucket bucket-user-images-store --lifecycle-configuration file://lifecycle-config.json
```

### 7. Verificar la Configuración

```bash
# Probar subida de archivo
echo "test" > test.txt
aws s3 cp test.txt s3://bucket-user-images-store/test/test.txt

# Probar descarga
aws s3 cp s3://bucket-user-images-store/test/test.txt downloaded-test.txt

# Probar eliminación
aws s3 rm s3://bucket-user-images-store/test/test.txt

# Limpiar archivos de prueba
rm test.txt downloaded-test.txt
```

## Explicación de la Policy IAM

### Permisos Incluidos:

1. **AllowImageOperations**: Permite operaciones básicas en objetos
   - `s3:PutObject`: Cargar imágenes
   - `s3:PutObjectAcl`: Modificar permisos de objeto
   - `s3:GetObject`: Descargar imágenes
   - `s3:GetObjectAcl`: Leer permisos de objeto
   - `s3:DeleteObject`: Eliminar imágenes

2. **AllowBucketOperations**: Permite operaciones en el bucket
   - `s3:ListBucket`: Listar objetos en el bucket
   - `s3:GetBucketLocation`: Obtener la región del bucket

3. **AllowPresignedURLOperations**: Permite generar URLs firmadas
   - Con condición para mantener objetos privados

### Seguridad:

- **Principio de menor privilegio**: Solo permisos necesarios
- **Scope limitado**: Solo al bucket específico
- **Objetos privados**: Condición para mantener ACL privada

## Troubleshooting

### Error: Access Denied
```bash
# Verificar permisos del usuario
aws iam list-attached-user-policies --user-name booky-s3-user

# Verificar policy del bucket
aws s3api get-bucket-policy --bucket bucket-user-images-store
```

### Error: Bucket does not exist
```bash
# Verificar que el bucket existe
aws s3 ls | grep bucket-user-images-store

# Verificar región del bucket
aws s3api get-bucket-location --bucket bucket-user-images-store
```

### Error: Token malformed
```bash
# Verificar credenciales
aws sts get-caller-identity

# Regenerar access keys si es necesario
aws iam create-access-key --user-name booky-s3-user
```

## Costos Estimados

- **Almacenamiento**: ~$0.023 por GB por mes
- **Requests**: ~$0.0004 por 1000 requests PUT/POST
- **Requests**: ~$0.0004 por 1000 requests GET
- **Transferencia**: Gratis para los primeros 100 GB por mes

## Backup y Monitoreo

### Habilitar Versioning (Recomendado):
```bash
aws s3api put-bucket-versioning --bucket bucket-user-images-store --versioning-configuration Status=Enabled
```

### Configurar CloudWatch Metrics:
```bash
aws s3api put-bucket-metrics-configuration --bucket bucket-user-images-store --id EntireBucket --metrics-configuration Id=EntireBucket
``` 