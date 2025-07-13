package com.uade.bookybe.infraestructure.adapter;

import com.uade.bookybe.core.port.ImageStoragePort;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "image.storage.strategy", havingValue = "s3")
public class S3ImageStorageAdapter implements ImageStoragePort {

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;
  private final String bucketName;

  @Value("${aws.s3.base-url:#{null}}")
  private String baseUrl;

  @Override
  public Optional<String> uploadImage(MultipartFile file, String folder) {
    try {
      log.info(
          "Uploading image to S3. File: {}, Folder: {}, Bucket: {}",
          file.getOriginalFilename(),
          folder,
          bucketName);

      // Generar un nombre único para el archivo
      String fileName = generateUniqueFileName(file.getOriginalFilename());
      String key = buildObjectKey(folder, fileName);

      // Determinar el content type
      String contentType = determineContentType(file);

      // Crear el request de upload
      PutObjectRequest putObjectRequest =
          PutObjectRequest.builder()
              .bucket(bucketName)
              .key(key)
              .contentType(contentType)
              .contentLength(file.getSize())
              .metadata(
                  java.util.Map.of(
                      "original-filename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                      "upload-timestamp", String.valueOf(System.currentTimeMillis())))
              .build();

      // Subir el archivo
      s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

      // Generar la URL pública
      String publicUrl = generatePublicUrl(key);
      log.info("Image uploaded successfully to S3. URL: {}", publicUrl);

      return Optional.of(publicUrl);

    } catch (IOException e) {
      log.error("Error reading file for S3 upload: {}", e.getMessage(), e);
      return Optional.empty();
    } catch (S3Exception e) {
      log.error("Error uploading image to S3: {}", e.getMessage(), e);
      return Optional.empty();
    } catch (Exception e) {
      log.error("Unexpected error uploading image to S3: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  @Override
  public boolean deleteImage(String imageUrl) {
    try {
      if (imageUrl == null || imageUrl.isBlank()) {
        return false;
      }

      // Extraer la key del objeto desde la URL
      String objectKey = extractObjectKeyFromUrl(imageUrl);
      if (objectKey == null) {
        log.warn("Could not extract object key from URL: {}", imageUrl);
        return false;
      }

      log.info("Deleting image from S3. Bucket: {}, Key: {}", bucketName, objectKey);

      DeleteObjectRequest deleteObjectRequest =
          DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build();

      s3Client.deleteObject(deleteObjectRequest);
      log.info("Image deleted successfully from S3");

      return true;

    } catch (S3Exception e) {
      log.error("Error deleting image from S3: {}", e.getMessage(), e);
      return false;
    } catch (Exception e) {
      log.error("Unexpected error deleting image from S3: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public String getOptimizedUrl(String imageUrl, Integer width, Integer height) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return imageUrl;
    }

    try {
      // Para S3, generamos una URL presigned con tiempo de expiración
      // Nota: S3 no tiene transformación de imágenes nativa como Cloudinary
      // Para optimización real necesitarías usar CloudFront + Lambda@Edge o similares
      
      String objectKey = extractObjectKeyFromUrl(imageUrl);
      if (objectKey == null) {
        return imageUrl;
      }

      GetObjectPresignRequest presignRequest =
          GetObjectPresignRequest.builder()
              .signatureDuration(Duration.ofHours(1)) // URL válida por 1 hora
              .getObjectRequest(builder -> builder.bucket(bucketName).key(objectKey))
              .build();

      PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
      String optimizedUrl = presignedRequest.url().toString();

      log.debug("Generated optimized URL for S3 object: {}", optimizedUrl);
      return optimizedUrl;

    } catch (Exception e) {
      log.error("Error generating optimized URL for S3 object: {}", e.getMessage(), e);
      return imageUrl;
    }
  }

  /**
   * Genera un nombre único para el archivo
   */
  private String generateUniqueFileName(String originalFileName) {
    String uuid = UUID.randomUUID().toString();
    
    if (originalFileName == null || originalFileName.isBlank()) {
      return uuid + ".jpg"; // Extensión por defecto
    }

    // Extraer la extensión del archivo original
    int lastDotIndex = originalFileName.lastIndexOf('.');
    String extension = lastDotIndex > 0 ? originalFileName.substring(lastDotIndex) : ".jpg";

    return uuid + extension;
  }

  /**
   * Construye la key del objeto S3
   */
  private String buildObjectKey(String folder, String fileName) {
    String normalizedFolder = folder != null && !folder.isBlank() ? folder.trim() : "images";
    
    // Asegurar que no empiece con /
    if (normalizedFolder.startsWith("/")) {
      normalizedFolder = normalizedFolder.substring(1);
    }
    
    // Asegurar que termine con /
    if (!normalizedFolder.endsWith("/")) {
      normalizedFolder += "/";
    }

    return normalizedFolder + fileName;
  }

  /**
   * Determina el content type del archivo
   */
  private String determineContentType(MultipartFile file) {
    String contentType = file.getContentType();
    
    if (contentType != null && contentType.startsWith("image/")) {
      return contentType;
    }

    // Determinar por extensión si no se puede detectar
    String fileName = file.getOriginalFilename();
    if (fileName != null) {
      fileName = fileName.toLowerCase();
      if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
        return "image/jpeg";
      } else if (fileName.endsWith(".png")) {
        return "image/png";
      } else if (fileName.endsWith(".gif")) {
        return "image/gif";
      } else if (fileName.endsWith(".webp")) {
        return "image/webp";
      }
    }

    return "image/jpeg"; // Por defecto
  }

  /**
   * Genera la URL pública del objeto
   */
  private String generatePublicUrl(String objectKey) {
    if (baseUrl != null && !baseUrl.isBlank()) {
      // Usar URL base personalizada (ej: CloudFront)
      return baseUrl.endsWith("/") ? baseUrl + objectKey : baseUrl + "/" + objectKey;
    }

    // Usar URL directa de S3
    GetUrlRequest getUrlRequest = GetUrlRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build();

    return s3Client.utilities().getUrl(getUrlRequest).toString();
  }

  /**
   * Extrae la key del objeto desde la URL
   */
  private String extractObjectKeyFromUrl(String imageUrl) {
    try {
      if (baseUrl != null && imageUrl.startsWith(baseUrl)) {
        // URL personalizada
        String key = imageUrl.substring(baseUrl.length());
        return key.startsWith("/") ? key.substring(1) : key;
      }

      // URL de S3 estándar: https://bucket.s3.region.amazonaws.com/key
      // o https://s3.region.amazonaws.com/bucket/key
      if (imageUrl.contains(".amazonaws.com/")) {
        String[] parts = imageUrl.split(".amazonaws.com/", 2);
        if (parts.length == 2) {
          String path = parts[1];
          
          // Si la URL incluye el bucket en el path, removerlo
          if (path.startsWith(bucketName + "/")) {
            return path.substring(bucketName.length() + 1);
          }
          
          return path;
        }
      }

      return null;

    } catch (Exception e) {
      log.error("Error extracting object key from URL {}: {}", imageUrl, e.getMessage());
      return null;
    }
  }
} 