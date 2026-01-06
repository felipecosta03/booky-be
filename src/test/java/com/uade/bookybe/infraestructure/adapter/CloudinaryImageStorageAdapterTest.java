package com.uade.bookybe.infraestructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.Uploader;
import com.uade.bookybe.core.port.ImageStoragePort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class CloudinaryImageStorageAdapterTest {

  @Mock private Cloudinary cloudinary;
  @Mock private Uploader uploader;

  private ImageStoragePort sut;

  @BeforeEach
  void setUp() {
    sut = new CloudinaryImageStorageAdapter(cloudinary);
  }

  // ---------------- uploadImage(MultipartFile) ----------------

  @Test
  void uploadImage_multipart_deberiaSubirYDevolverSecureUrl() throws Exception {
    // given
    MultipartFile file =
        new MockMultipartFile(
            "file", "pic.jpg", "image/jpeg", "imgbytes".getBytes(StandardCharsets.UTF_8));
    given(cloudinary.uploader()).willReturn(uploader);

    given(uploader.upload(any(byte[].class), anyMap()))
        .willReturn(
            Map.of(
                "secure_url", "https://res.cloudinary.com/x/image/upload/v1/booky/users/pic.webp"));

    // when
    Optional<String> result = sut.uploadImage(file, "booky/users");

    // then
    assertTrue(result.isPresent());
    assertEquals("https://res.cloudinary.com/x/image/upload/v1/booky/users/pic.webp", result.get());

    then(uploader).should().upload(any(byte[].class), anyMap());
  }

  @Test
  void uploadImage_multipart_deberiaRetornarEmpty_cuandoUploaderLanzaIOException()
      throws Exception {
    given(cloudinary.uploader()).willReturn(uploader);
    // given
    MultipartFile file =
        new MockMultipartFile(
            "file", "pic.jpg", "image/jpeg", "imgbytes".getBytes(StandardCharsets.UTF_8));

    given(uploader.upload(any(byte[].class), anyMap())).willThrow(new IOException("boom"));

    // when
    Optional<String> result = sut.uploadImage(file, "booky/users");

    // then
    assertTrue(result.isEmpty());
  }

  // ---------------- uploadImage(base64) ----------------

  @Test
  void uploadImage_base64_deberiaRetornarEmpty_cuandoBase64Null() throws Exception {
    // when

    Optional<String> result = sut.uploadImage((String) null, "booky/base64");

    // then
    assertTrue(result.isEmpty());
    then(uploader).shouldHaveNoInteractions();
  }

  @Test
  void uploadImage_base64_deberiaRetornarEmpty_cuandoBase64Blank() throws Exception {
    // when
    Optional<String> result = sut.uploadImage("   ", "booky/base64");

    // then
    assertTrue(result.isEmpty());
    then(uploader).shouldHaveNoInteractions();
  }

  @Test
  void uploadImage_base64_deberiaRetornarEmpty_cuandoBase64Invalido() throws Exception {
    // given
    String invalid = "data:image/png;base64,%%%NOT_BASE64%%%";

    // when
    Optional<String> result = sut.uploadImage(invalid, "booky/base64");

    // then
    assertTrue(result.isEmpty());
    then(uploader).shouldHaveNoInteractions();
}

  @Test
  void uploadImage_base64_deberiaRetornarEmpty_cuandoUploaderLanzaException() throws Exception {
    given(cloudinary.uploader()).willReturn(uploader);
    // given
    String base64 = Base64.getEncoder().encodeToString("imgbytes".getBytes(StandardCharsets.UTF_8));
    given(uploader.upload(any(byte[].class), anyMap())).willThrow(new RuntimeException("boom"));

    // when
    Optional<String> result = sut.uploadImage(base64, "booky/base64");

    // then
    assertTrue(result.isEmpty());
  }

  // ---------------- deleteImage ----------------

  @Test
  void deleteImage_deberiaRetornarFalse_cuandoUrlNullOBlank() {
    assertFalse(sut.deleteImage(null));
    assertFalse(sut.deleteImage("   "));
    then(uploader).shouldHaveNoInteractions();
  }

  @Test
  void deleteImage_deberiaRetornarFalse_cuandoUrlNoEsCloudinary() {

    boolean result = sut.deleteImage("https://example.com/a/b/c.png");
    assertFalse(result);
    then(uploader).shouldHaveNoInteractions();
  }

  @Test
  void deleteImage_deberiaRetornarTrue_cuandoDestroyResultOk() throws Exception {
    // given

    given(cloudinary.uploader()).willReturn(uploader);
    String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/booky/users/img.webp";
    given(uploader.destroy(eq("booky/users/img"), anyMap())).willReturn(Map.of("result", "ok"));

    // when
    boolean result = sut.deleteImage(url);

    // then
    assertTrue(result);
    then(uploader).should().destroy(eq("booky/users/img"), anyMap());
  }

  @Test
  void deleteImage_deberiaRetornarFalse_cuandoDestroyNoOk() throws Exception {
    given(cloudinary.uploader()).willReturn(uploader);
    // given
    String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/booky/users/img.webp";
    given(uploader.destroy(eq("booky/users/img"), anyMap()))
        .willReturn(Map.of("result", "not found"));

    // when
    boolean result = sut.deleteImage(url);

    // then
    assertFalse(result);
  }

  @Test
  void deleteImage_deberiaRetornarFalse_cuandoDestroyLanzaIOException() throws Exception {
    // given
    given(cloudinary.uploader()).willReturn(uploader);
    String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/booky/users/img.webp";
    given(uploader.destroy(eq("booky/users/img"), anyMap())).willThrow(new IOException("boom"));

    // when
    boolean result = sut.deleteImage(url);

    // then
    assertFalse(result);
  }

  // ---------------- getOptimizedUrl ----------------

  @Test
  void getOptimizedUrl_deberiaRetornarOriginal_cuandoUrlNullOBlank() {
    assertNull(sut.getOptimizedUrl(null, 100, 100));
    assertEquals("   ", sut.getOptimizedUrl("   ", 100, 100));
  }

  @Test
  void getOptimizedUrl_deberiaRetornarOriginal_cuandoNoEsCloudinary() {
    String url = "https://example.com/x.png";
    assertEquals(url, sut.getOptimizedUrl(url, 100, 100));
  }

  @Test
  void getOptimizedUrl_deberiaGenerarUrlConTransformation_cuandoEsCloudinary() {
    // given
    String url = "https://res.cloudinary.com/demo/image/upload/v1234567890/booky/users/img.webp";

    // armamos la cadena cloudinary.url().transformation(...).generate(publicId)
    var urlBuilder = mock(com.cloudinary.Url.class);
    var urlBuilder2 = mock(com.cloudinary.Url.class);

    given(cloudinary.url()).willReturn(urlBuilder);
    given(urlBuilder.transformation(any(Transformation.class))).willReturn(urlBuilder2);
    given(urlBuilder2.generate(eq("booky/users/img")))
        .willReturn(
            "https://res.cloudinary.com/demo/image/upload/c_fill,h_80,q_auto:good,w_120/booky/users/img");

    // when
    String optimized = sut.getOptimizedUrl(url, 120, 80);

    // then
    assertNotNull(optimized);
    assertTrue(optimized.contains("booky/users/img"));
    then(cloudinary).should().url();
    then(urlBuilder).should().transformation(any(Transformation.class));
    then(urlBuilder2).should().generate(eq("booky/users/img"));
  }
}
