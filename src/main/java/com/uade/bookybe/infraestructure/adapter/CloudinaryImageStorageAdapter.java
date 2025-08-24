package com.uade.bookybe.infraestructure.adapter;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.uade.bookybe.core.port.ImageStoragePort;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "image.storage.strategy",
    havingValue = "cloudinary",
    matchIfMissing = true)
public class CloudinaryImageStorageAdapter implements ImageStoragePort {

  private final Cloudinary cloudinary;

  @Override
  public Optional<String> uploadImage(MultipartFile file, String folder) {
    try {
      log.info(
          "Uploading image to Cloudinary. File: {}, Folder: {}",
          file.getOriginalFilename(),
          folder);

      Map<String, Object> uploadParams =
          ObjectUtils.asMap(
              "resource_type",
              "image",
              "folder",
              folder != null ? folder : "booky/users",
              "use_filename",
              true,
              "unique_filename",
              true,
              "overwrite",
              false,
              "quality",
              "auto:good",
              "format",
              "webp");

      Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(), uploadParams);
      String imageUrl = (String) uploadResult.get("secure_url");

      log.info("Image uploaded successfully. URL: {}", imageUrl);
      return Optional.of(imageUrl);

    } catch (IOException e) {
      log.error("Error uploading image to Cloudinary: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  @Override
  public boolean deleteImage(String imageUrl) {
    try {
      if (imageUrl == null || imageUrl.isBlank()) {
        return false;
      }

      // Extraer public_id de la URL de Cloudinary
      String publicId = extractPublicIdFromUrl(imageUrl);
      if (publicId == null) {
        log.warn("Could not extract public_id from URL: {}", imageUrl);
        return false;
      }

      log.info("Deleting image from Cloudinary. Public ID: {}", publicId);

      Map<?, ?> deleteResult = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
      String result = (String) deleteResult.get("result");

      boolean success = "ok".equals(result);
      log.info("Image deletion result: {}", result);

      return success;

    } catch (IOException e) {
      log.error("Error deleting image from Cloudinary: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public String getOptimizedUrl(String imageUrl, Integer width, Integer height) {
    if (imageUrl == null || imageUrl.isBlank()) {
      return imageUrl;
    }

    try {
      // Si es una URL de Cloudinary, generar URL optimizada
      if (imageUrl.contains("cloudinary.com")) {
        String publicId = extractPublicIdFromUrl(imageUrl);
        if (publicId != null) {
          Transformation transformation =
              new Transformation()
                  .width(width != null ? width : 300)
                  .height(height != null ? height : 300)
                  .crop("fill")
                  .quality("auto:good");

          return cloudinary.url().transformation(transformation).generate(publicId);
        }
      }

      // Si no es de Cloudinary, devolver la URL original
      return imageUrl;

    } catch (Exception e) {
      log.error("Error generating optimized URL: {}", e.getMessage(), e);
      return imageUrl;
    }
  }

  /**
   * Extrae el public_id de una URL de Cloudinary Ejemplo:
   * https://res.cloudinary.com/demo/image/upload/v1234567890/folder/image.jpg Retorna: folder/image
   */
  private String extractPublicIdFromUrl(String imageUrl) {
    try {
      if (!imageUrl.contains("cloudinary.com")) {
        return null;
      }

      // Dividir la URL y buscar la parte después de "/upload/"
      String[] parts = imageUrl.split("/upload/");
      if (parts.length < 2) {
        return null;
      }

      String afterUpload = parts[1];

      // Remover la versión si existe (v1234567890/)
      if (afterUpload.startsWith("v")) {
        String[] versionParts = afterUpload.split("/", 2);
        if (versionParts.length > 1) {
          afterUpload = versionParts[1];
        }
      }

      // Remover la extensión del archivo
      int lastDotIndex = afterUpload.lastIndexOf('.');
      if (lastDotIndex > 0) {
        afterUpload = afterUpload.substring(0, lastDotIndex);
      }

      return afterUpload;

    } catch (Exception e) {
      log.error("Error extracting public_id from URL {}: {}", imageUrl, e.getMessage());
      return null;
    }
  }
}
