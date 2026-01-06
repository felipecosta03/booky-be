package com.uade.bookybe.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageAdapterTest {

  private static final String BUCKET = "my-bucket";

  @Mock private S3Client s3Client;
  @Mock private S3Presigner s3Presigner;
  @Mock private MultipartFile multipartFile;

  @Captor private ArgumentCaptor<PutObjectRequest> putCaptor;
  @Captor private ArgumentCaptor<DeleteObjectRequest> deleteCaptor;
  @Captor private ArgumentCaptor<GetObjectPresignRequest> presignCaptor;

  private S3ImageStorageAdapter sut;

  @BeforeEach
  void setUp() {
    sut = new S3ImageStorageAdapter(s3Client, s3Presigner, BUCKET);
    ReflectionTestUtils.setField(sut, "baseUrl", null);
  }

  // ---------------- uploadImage(MultipartFile, folder) ----------------

  @Test
  void uploadImage_deberiaSubirYRetornarUrl_cuandoOk_yContentTypeVieneDelFile() throws Exception {
    // given
    given(multipartFile.getOriginalFilename()).willReturn("foto.png");
    given(multipartFile.getContentType()).willReturn("image/png");
    given(multipartFile.getSize()).willReturn(3L);
    given(multipartFile.getBytes()).willReturn(new byte[] {1, 2, 3});

    // putObject OK
    willReturn(PutObjectResponse.builder().eTag("etag").build())
        .given(s3Client)
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));

    // utilities.getUrl
    S3Utilities utils = mock(S3Utilities.class);
    given(s3Client.utilities()).willReturn(utils);
    given(utils.getUrl(any(GetUrlRequest.class))).willReturn(new URL("https://s3-url/generated"));

    // when
    Optional<String> result = sut.uploadImage(multipartFile, "booky/users");

    // then
    assertTrue(result.isPresent());
    assertEquals("https://s3-url/generated", result.get());

    then(s3Client).should().putObject(putCaptor.capture(), any(RequestBody.class));
    PutObjectRequest req = putCaptor.getValue();

    assertEquals(BUCKET, req.bucket());
    assertTrue(req.key().startsWith("booky/users/"));
    assertTrue(req.key().endsWith(".png"));
    assertEquals("image/png", req.contentType());
    assertEquals(3L, req.contentLength());
  }

  @Test
  void uploadImage_deberiaInferirContentTypePorExtension_cuandoFileNoTraeContentType()
      throws Exception {
    // given
    given(multipartFile.getOriginalFilename()).willReturn("foto.webp");
    given(multipartFile.getContentType()).willReturn(null);
    given(multipartFile.getSize()).willReturn(1L);
    given(multipartFile.getBytes()).willReturn(new byte[] {9});

    willReturn(PutObjectResponse.builder().eTag("etag").build())
        .given(s3Client)
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));

    S3Utilities utils = mock(S3Utilities.class);
    given(s3Client.utilities()).willReturn(utils);
    given(utils.getUrl(any(GetUrlRequest.class))).willReturn(new URL("https://s3-url/generated"));

    // when
    Optional<String> result = sut.uploadImage(multipartFile, "images");

    // then
    assertTrue(result.isPresent());

    then(s3Client).should().putObject(putCaptor.capture(), any(RequestBody.class));
    PutObjectRequest req = putCaptor.getValue();
    assertEquals("image/webp", req.contentType());
    assertTrue(req.key().startsWith("images/"));
    assertTrue(req.key().endsWith(".webp"));
  }

  @Test
  void uploadImage_deberiaNormalizarFolder_conSlashInicial_ySinSlashFinal() throws Exception {
    // given
    given(multipartFile.getOriginalFilename()).willReturn("a.jpg");
    given(multipartFile.getContentType()).willReturn("image/jpeg");
    given(multipartFile.getSize()).willReturn(1L);
    given(multipartFile.getBytes()).willReturn(new byte[] {1});

    willReturn(PutObjectResponse.builder().eTag("etag").build())
        .given(s3Client)
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));

    S3Utilities utils = mock(S3Utilities.class);
    given(s3Client.utilities()).willReturn(utils);
    given(utils.getUrl(any(GetUrlRequest.class))).willReturn(new URL("https://s3-url/generated"));

    // when
    Optional<String> result = sut.uploadImage(multipartFile, "/folder");

    // then
    assertTrue(result.isPresent());

    then(s3Client).should().putObject(putCaptor.capture(), any(RequestBody.class));
    assertTrue(putCaptor.getValue().key().startsWith("folder/"));
  }

  @Test
  void uploadImage_deberiaRetornarEmpty_cuandoIOExceptionAlLeerBytes() throws Exception {
    // given
    given(multipartFile.getOriginalFilename()).willReturn("a.jpg");
    given(multipartFile.getContentType()).willReturn("image/jpeg");
    given(multipartFile.getSize()).willReturn(10L);
    given(multipartFile.getBytes()).willThrow(new IOException("read fail"));

    // when
    Optional<String> result = sut.uploadImage(multipartFile, "folder");

    // then
    assertTrue(result.isEmpty());
    then(s3Client).should(never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void uploadImage_deberiaRetornarEmpty_cuandoS3Exception() throws Exception {
    // given
    given(multipartFile.getOriginalFilename()).willReturn("a.jpg");
    given(multipartFile.getContentType()).willReturn("image/jpeg");
    given(multipartFile.getSize()).willReturn(1L);
    given(multipartFile.getBytes()).willReturn(new byte[] {1});

    willThrow(S3Exception.builder().message("boom").build())
        .given(s3Client)
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));

    // when
    Optional<String> result = sut.uploadImage(multipartFile, "folder");

    // then
    assertTrue(result.isEmpty());
  }

  // ---------------- uploadImage(base64, folder) ----------------

  @Test
  void uploadBase64_deberiaRetornarEmpty_cuandoBase64NullOVacio() {
    assertTrue(sut.uploadImage((String) null, "folder").isEmpty());
    assertTrue(sut.uploadImage("   ", "folder").isEmpty());
  }

  @Test
  void uploadBase64_deberiaRetornarEmpty_cuandoBase64Invalido() {
    assertTrue(sut.uploadImage("%%%", "folder").isEmpty());
  }

  @Test
  void uploadBase64_deberiaSubirYRetornarUrl_cuandoOk_conHeaderPng() throws Exception {
    // given
    String payload = Base64.getEncoder().encodeToString("hola".getBytes());
    String base64 = "data:image/png;base64," + payload;

    willReturn(PutObjectResponse.builder().eTag("etag").build())
        .given(s3Client)
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));

    S3Utilities utils = mock(S3Utilities.class);
    given(s3Client.utilities()).willReturn(utils);
    given(utils.getUrl(any(GetUrlRequest.class))).willReturn(new URL("https://s3-url/generated"));

    // when
    Optional<String> result = sut.uploadImage(base64, "booky/users");

    // then
    assertTrue(result.isPresent());
    assertEquals("https://s3-url/generated", result.get());

    then(s3Client).should().putObject(putCaptor.capture(), any(RequestBody.class));
    PutObjectRequest req = putCaptor.getValue();
    assertEquals("image/png", req.contentType());
    assertTrue(req.key().startsWith("booky/users/"));
    assertTrue(req.key().endsWith(".png"));
  }

  @Test
  void uploadBase64_deberiaSubirComoJpegPorDefecto_cuandoSinHeader() throws Exception {
    // given
    String base64 = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});

    willReturn(PutObjectResponse.builder().eTag("etag").build())
        .given(s3Client)
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));

    S3Utilities utils = mock(S3Utilities.class);
    given(s3Client.utilities()).willReturn(utils);
    given(utils.getUrl(any(GetUrlRequest.class))).willReturn(new URL("https://s3-url/generated"));

    // when
    Optional<String> result = sut.uploadImage(base64, "folder");

    // then
    assertTrue(result.isPresent());

    then(s3Client).should().putObject(putCaptor.capture(), any(RequestBody.class));
    PutObjectRequest req = putCaptor.getValue();
    assertEquals("image/jpeg", req.contentType());
    assertTrue(req.key().endsWith(".jpg"));
  }

  // ---------------- deleteImage ----------------

  @Test
  void deleteImage_deberiaRetornarFalse_cuandoUrlNullOBlank() {
    assertFalse(sut.deleteImage(null));
    assertFalse(sut.deleteImage("   "));
  }

  @Test
  void deleteImage_deberiaEliminar_cuandoUsaBaseUrlPersonalizada() {
    // given
    ReflectionTestUtils.setField(sut, "baseUrl", "https://cdn.example.com");
    String url = "https://cdn.example.com/folder/a.jpg";

    willReturn(DeleteObjectResponse.builder().build())
        .given(s3Client)
        .deleteObject(any(DeleteObjectRequest.class));

    // when
    boolean result = sut.deleteImage(url);

    // then
    assertTrue(result);

    then(s3Client).should().deleteObject(deleteCaptor.capture());
    DeleteObjectRequest req = deleteCaptor.getValue();
    assertEquals(BUCKET, req.bucket());
    assertEquals("folder/a.jpg", req.key());
  }

  @Test
  void deleteImage_deberiaEliminar_cuandoUrlS3IncluyeBucketEnPath() {
    // given
    String url = "https://s3.sa-east-1.amazonaws.com/" + BUCKET + "/folder/a.jpg";

    willReturn(DeleteObjectResponse.builder().build())
        .given(s3Client)
        .deleteObject(any(DeleteObjectRequest.class));

    // when
    boolean result = sut.deleteImage(url);

    // then
    assertTrue(result);

    then(s3Client).should().deleteObject(deleteCaptor.capture());
    assertEquals("folder/a.jpg", deleteCaptor.getValue().key());
  }

  @Test
  void deleteImage_deberiaEliminar_cuandoUrlS3VirtualHostedStyle() {
    // given
    String url = "https://" + BUCKET + ".s3.sa-east-1.amazonaws.com/folder/a.jpg";

    willReturn(DeleteObjectResponse.builder().build())
        .given(s3Client)
        .deleteObject(any(DeleteObjectRequest.class));

    // when
    boolean result = sut.deleteImage(url);

    // then
    assertTrue(result);

    then(s3Client).should().deleteObject(deleteCaptor.capture());
    assertEquals("folder/a.jpg", deleteCaptor.getValue().key());
  }

  @Test
  void deleteImage_deberiaRetornarFalse_cuandoNoPuedeExtraerKey() {
    // given
    String url = "https://example.com/not-s3/folder/a.jpg";

    // when
    boolean result = sut.deleteImage(url);

    // then
    assertFalse(result);
    then(s3Client).should(never()).deleteObject(any(DeleteObjectRequest.class));
  }

  // ---------------- getOptimizedUrl (presigned) ----------------

  @Test
  void getOptimizedUrl_deberiaRetornarPresignedUrl_cuandoPuedeExtraerKey() throws Exception {
    // given
    String url = "https://" + BUCKET + ".s3.sa-east-1.amazonaws.com/folder/a.jpg";

    PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
    given(presigned.url()).willReturn(new URL("https://presigned/url"));

    given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presigned);

    // when
    String optimized = sut.getOptimizedUrl(url, 100, 100);

    // then
    assertEquals("https://presigned/url", optimized);

    then(s3Presigner).should().presignGetObject(presignCaptor.capture());
    GetObjectPresignRequest req = presignCaptor.getValue();

    assertEquals(Duration.ofHours(1), req.signatureDuration());
    assertEquals(BUCKET, req.getObjectRequest().bucket());
    assertEquals("folder/a.jpg", req.getObjectRequest().key());
  }

  @Test
  void getOptimizedUrl_deberiaDevolverOriginal_cuandoNoPuedeExtraerKey() {
    // given
    String url = "https://example.com/not-s3/a.jpg";

    // when
    String optimized = sut.getOptimizedUrl(url, 100, 100);

    // then
    assertEquals(url, optimized);
    then(s3Presigner).should(never()).presignGetObject(any(GetObjectPresignRequest.class));
  }

  @Test
  void getOptimizedUrl_deberiaDevolverOriginal_cuandoPresignerFalla() {
    // given
    String url = "https://" + BUCKET + ".s3.sa-east-1.amazonaws.com/folder/a.jpg";
    given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
        .willThrow(new RuntimeException("boom"));

    // when
    String optimized = sut.getOptimizedUrl(url, 100, 100);

    // then
    assertEquals(url, optimized);
  }
}
