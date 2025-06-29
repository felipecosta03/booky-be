package com.uade.bookybe.core.port;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Port para el servicio de almacenamiento de imágenes. Permite cambiar fácilmente entre diferentes
 * proveedores (Cloudinary, S3, etc.)
 */
public interface ImageStoragePort {

  /**
   * Sube una imagen al servicio de almacenamiento
   *
   * @param file Archivo de imagen a subir
   * @param folder Carpeta donde almacenar la imagen (opcional)
   * @return URL pública de la imagen subida, o Optional.empty() si falla
   */
  Optional<String> uploadImage(MultipartFile file, String folder);

  /**
   * Elimina una imagen del servicio de almacenamiento
   *
   * @param imageUrl URL de la imagen a eliminar
   * @return true si se eliminó correctamente, false en caso contrario
   */
  boolean deleteImage(String imageUrl);

  /**
   * Genera una URL optimizada para diferentes tamaños
   *
   * @param imageUrl URL original de la imagen
   * @param width Ancho deseado
   * @param height Alto deseado
   * @return URL optimizada
   */
  String getOptimizedUrl(String imageUrl, Integer width, Integer height);
}
