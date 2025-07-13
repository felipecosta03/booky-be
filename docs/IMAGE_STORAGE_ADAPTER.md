# Image Storage Adapter Configuration

## Overview

The application supports multiple image storage strategies through the `ImageStoragePort` interface. You can choose between **Cloudinary** (default) and **AWS S3** using configuration properties.

## Supported Strategies

### 1. Cloudinary (Default)
- **Best for**: Quick setup, image transformations, and optimization
- **Features**: Automatic optimization, resizing, format conversion
- **Configuration**: Simple API key setup

### 2. AWS S3
- **Best for**: Enterprise environments, cost control, integration with AWS services
- **Features**: Scalable storage, presigned URLs, IAM role support
- **Configuration**: Supports both credentials and IAM roles

## Configuration

### Strategy Selection

The storage strategy is controlled by the `image.storage.strategy` property:

```yaml
image:
  storage:
    strategy: cloudinary  # or s3
```

### Environment Variable

```bash
IMAGE_STORAGE_STRATEGY=cloudinary  # Default
IMAGE_STORAGE_STRATEGY=s3         # Use S3
```

## Cloudinary Configuration

### Development (Local)

```yaml
# application.yml (defaults)
cloudinary:
  cloud-name: dfsfkyyx7
  api-key: 438652139556741
  api-secret: 1QsWsAAwvBelPw1kc4TWdLNSrjc
```

### Production

```yaml
# application-prod.yml
cloudinary:
  cloud-name: ${CLOUDINARY_CLOUD_NAME:your-production-cloud-name}
  api-key: ${CLOUDINARY_API_KEY:your-production-api-key}
  api-secret: ${CLOUDINARY_API_SECRET:your-production-api-secret}
```

### Environment Variables

```bash
CLOUDINARY_CLOUD_NAME=your-cloud-name
CLOUDINARY_API_KEY=your-api-key
CLOUDINARY_API_SECRET=your-api-secret
```

## AWS S3 Configuration

### YAML Configuration

```yaml
# application.yml
aws:
  s3:
    access-key: ${AWS_S3_ACCESS_KEY:#{null}}
    secret-key: ${AWS_S3_SECRET_KEY:#{null}}
    region: ${AWS_S3_REGION:us-east-1}
    bucket: ${AWS_S3_BUCKET:bucket-user-images-store}
    base-url: ${AWS_S3_BASE_URL:#{null}}
```

### Environment Variables

```bash
# Required
AWS_S3_REGION=us-east-1
AWS_S3_BUCKET=bucket-user-images-store

# Optional (if not using IAM roles)
AWS_S3_ACCESS_KEY=your-access-key
AWS_S3_SECRET_KEY=your-secret-key

# Optional (for CloudFront or custom domain)
AWS_S3_BASE_URL=https://cdn.yoursite.com
```

### IAM Role Configuration (Recommended for Production)

Instead of access keys, use IAM roles:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject"
      ],
      "Resource": "arn:aws:s3:::bucket-user-images-store/*"
    }
  ]
}
```

## Docker Compose Configuration

### Development (Local)

```yaml
# docker-compose.yml
environment:
  IMAGE_STORAGE_STRATEGY: cloudinary
  # S3 is commented out for development
  # AWS_S3_ACCESS_KEY: your-access-key
  # AWS_S3_SECRET_KEY: your-secret-key
  # AWS_S3_REGION: us-east-1
  # AWS_S3_BUCKET: bucket-user-images-store
```

### Production (EC2)

```yaml
# docker-compose-ec2.yml
environment:
  IMAGE_STORAGE_STRATEGY: ${IMAGE_STORAGE_STRATEGY:-cloudinary}
  # S3 Configuration
  AWS_S3_ACCESS_KEY: ${AWS_S3_ACCESS_KEY:-}
  AWS_S3_SECRET_KEY: ${AWS_S3_SECRET_KEY:-}
  AWS_S3_REGION: ${AWS_S3_REGION:-us-east-1}
  AWS_S3_BUCKET: ${AWS_S3_BUCKET:-bucket-user-images-store}
  AWS_S3_BASE_URL: ${AWS_S3_BASE_URL:-}
```

## Implementation Details

### Cloudinary Adapter

- **File**: `CloudinaryImageStorageAdapter.java`
- **Condition**: `@ConditionalOnProperty(name = "image.storage.strategy", havingValue = "cloudinary", matchIfMissing = true)`
- **Features**:
  - Automatic image optimization
  - Multiple format support (WebP conversion)
  - Real-time transformations
  - Built-in CDN

### S3 Adapter

- **File**: `S3ImageStorageAdapter.java`
- **Condition**: `@ConditionalOnProperty(name = "image.storage.strategy", havingValue = "s3")`
- **Features**:
  - Unique filename generation (UUID)
  - Content type detection
  - Presigned URLs for optimization
  - Support for custom domains (CloudFront)
  - IAM role support

## API Usage

The `ImageStoragePort` interface provides consistent methods regardless of the storage strategy:

```java
@Autowired
private ImageStoragePort imageStoragePort;

// Upload image
Optional<String> imageUrl = imageStoragePort.uploadImage(file, "user-profiles");

// Delete image
boolean deleted = imageStoragePort.deleteImage(imageUrl);

// Get optimized URL
String optimizedUrl = imageStoragePort.getOptimizedUrl(imageUrl, 300, 300);
```

## File Organization

### Cloudinary
- Default folder: `booky/users`
- Structure: `folder/unique_filename.ext`
- Example: `booky/users/profile_123.webp`

### S3
- Default folder: `images`
- Structure: `folder/uuid.ext`
- Example: `user-profiles/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg`

## Performance Considerations

### Cloudinary
- ✅ Built-in CDN
- ✅ Automatic optimization
- ✅ Real-time transformations
- ❌ Higher cost for large volumes

### S3
- ✅ Lower storage costs
- ✅ Highly scalable
- ✅ Integration with AWS services
- ❌ Requires CloudFront for CDN
- ❌ No built-in image transformations

## Migration

### From Cloudinary to S3

1. Set environment variable: `IMAGE_STORAGE_STRATEGY=s3`
2. Configure S3 credentials and bucket
3. Restart application
4. New uploads will use S3
5. Consider migrating existing images gradually

### From S3 to Cloudinary

1. Set environment variable: `IMAGE_STORAGE_STRATEGY=cloudinary`
2. Configure Cloudinary credentials
3. Restart application
4. New uploads will use Cloudinary

## Testing

### Test Current Strategy

```bash
# Check which strategy is active
curl http://localhost:8080/actuator/health

# Upload a test image
curl -X POST http://localhost:8080/upload \
  -F "file=@test-image.jpg" \
  -F "folder=test"
```

### Test S3 Configuration

```bash
# Test with S3
IMAGE_STORAGE_STRATEGY=s3 \
AWS_S3_BUCKET=bucket-user-images-store \
AWS_S3_REGION=us-east-1 \
java -jar app.jar
```

## Troubleshooting

### Common Issues

1. **Strategy not switching**: Check `IMAGE_STORAGE_STRATEGY` environment variable
2. **S3 access denied**: Verify IAM permissions or access keys
3. **S3 307 redirect error**: This is caused by the URL connection client not handling S3 redirects properly. The application uses Apache HTTP client to resolve this issue.
4. **Cloudinary upload fails**: Check API credentials and quotas
5. **Images not loading**: Verify bucket policies and CORS settings

### Debug Commands

```bash
# Check active beans
curl http://localhost:8080/actuator/beans | grep -i "ImageStorage"

# Check environment variables
docker exec booky-backend env | grep -E "(IMAGE_STORAGE|AWS_S3|CLOUDINARY)"

# Check application logs
docker logs booky-backend | grep -i "image\|storage\|s3\|cloudinary"
```

## Security Best Practices

### S3 Security

1. Use IAM roles instead of access keys in production
2. Enable S3 bucket encryption
3. Configure proper CORS settings
4. Use presigned URLs for temporary access
5. Implement bucket policies to restrict access

### Cloudinary Security

1. Rotate API secrets regularly
2. Use signed URLs for sensitive content
3. Configure upload presets with restrictions
4. Monitor usage and set quotas

## Cost Optimization

### S3 Cost Tips

1. Use S3 Intelligent-Tiering for automatic cost optimization
2. Implement lifecycle policies for old images
3. Use CloudFront for global distribution
4. Monitor usage with CloudWatch

### Cloudinary Cost Tips

1. Set up usage alerts
2. Use auto-optimization features
3. Implement image format optimization
4. Consider volume pricing tiers 