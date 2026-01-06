package com.uade.bookybe.core.service.gateway;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import com.uade.bookybe.config.OpenAIConfig;
import com.uade.bookybe.core.model.dto.ImageResult;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class OpenAIClientTest {

  @Mock private WebClient webClient;
  @Mock private OpenAIConfig openAIConfig;
  @Mock private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

  @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
  @Mock private WebClient.RequestBodySpec requestBodySpec;
  @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
  @Mock private WebClient.ResponseSpec responseSpec;

  @InjectMocks private OpenAIClient sut;

  @Captor private ArgumentCaptor<Object> bodyCaptor;

  // ---------------- craftPromptWithGPT ----------------

  @Test
  void craftPromptWithGPT_deberiaRetornarContenidoTrim_cuandoRespuestaValida() throws Exception {
    // given
    given(openAIConfig.getChatModel()).willReturn("gpt-test");
    given(openAIConfig.getMaxRetries()).willReturn(0);
    given(openAIConfig.getTimeout()).willReturn(Duration.ofSeconds(2));

    Object chatResponse = buildChatResponseWithContent("   hola mundo   ");

    stubWebClientPostChain("/chat/completions", chatResponse);

    // when
    String result = sut.craftPromptWithGPT("SYS", "USR");

    // then
    assertEquals("hola mundo", result);

    then(webClient).should().post();
    then(requestBodyUriSpec).should().uri("/chat/completions");
    then(requestBodySpec).should().bodyValue(bodyCaptor.capture());

    Object request = bodyCaptor.getValue();
    assertNotNull(request);

    // assert request.model == chatModel
    assertEquals("gpt-test", getField(request, "model"));
    // assert maxTokens == 500
    assertEquals(500, getField(request, "maxTokens"));
    // assert temperature == 0.7
    assertEquals(0.7, (Double) getField(request, "temperature"));

    @SuppressWarnings("unchecked")
    List<Object> messages = (List<Object>) getField(request, "messages");
    assertEquals(2, messages.size());
    assertEquals("system", getField(messages.get(0), "role"));
    assertEquals("SYS", getField(messages.get(0), "content"));
    assertEquals("user", getField(messages.get(1), "role"));
    assertEquals("USR", getField(messages.get(1), "content"));
  }

  // ---------------- generateImage ----------------

  @Test
  void generateImage_deberiaRetornarBase64_cuandoReturnBase64True_ySizeNoGrande() throws Exception {
    // given
    given(openAIConfig.getImageModel()).willReturn("img-model");
    given(openAIConfig.getMaxRetries()).willReturn(0);

    Object imageResponse = buildImageResponse("http://img", "BASE64DATA", "rev");
    stubWebClientPostChain("/images/generations", imageResponse);

    // when
    ImageResult result = sut.generateImage("prompt", "2048x1024", null, true);

    // then
    assertNotNull(result);
    assertEquals("http://img", result.getUrl());
    assertEquals("BASE64DATA", result.getBase64()); // should keep base64 for non-large
    assertEquals("rev", result.getRevisedPrompt());
    assertNotNull(result.getResponseTimeMs());

    then(requestBodySpec).should().bodyValue(bodyCaptor.capture());
    Object req = bodyCaptor.getValue();

    assertEquals("img-model", getField(req, "model"));
    assertEquals("prompt", getField(req, "prompt"));
    assertEquals(1, getField(req, "n"));
    assertEquals("2048x1024", getField(req, "size"));
    assertEquals("standard", getField(req, "quality"));
    assertEquals("vivid", getField(req, "style"));
    assertEquals("b64_json", getField(req, "responseFormat"));
  }

  @Test
  void generateImage_deberiaForzarUrl_cuandoReturnBase64True_ySizeGrande() throws Exception {
    // given
    given(openAIConfig.getImageModel()).willReturn("img-model");
    given(openAIConfig.getMaxRetries()).willReturn(0);

    Object imageResponse = buildImageResponse("http://img", "BASE64DATA", "rev");
    stubWebClientPostChain("/images/generations", imageResponse);

    // when
    ImageResult result = sut.generateImage("prompt", "4096x2048", null, true);

    // then
    assertNotNull(result);
    assertEquals("http://img", result.getUrl());
    assertNull(result.getBase64(), "Para size grande debe forzar URL y base64 null");

    then(requestBodySpec).should().bodyValue(bodyCaptor.capture());
    Object req = bodyCaptor.getValue();
    assertEquals("url", getField(req, "responseFormat"));
  }

  // ---------------- helpers: WebClient chain ----------------
  @SuppressWarnings({"unchecked", "rawtypes"})
  private void stubWebClientPostChain(String expectedUri, Object responseObject) {
    given(webClient.post()).willReturn(requestBodyUriSpec);
    given(requestBodyUriSpec.uri(expectedUri)).willReturn(requestBodySpec);

    // Evita el problema de capture#1 vs capture#2 en RequestHeadersSpec<?>
    doReturn((WebClient.RequestHeadersSpec) requestHeadersSpec)
        .when(requestBodySpec)
        .bodyValue(any());

    given(requestHeadersSpec.retrieve()).willReturn(responseSpec);

    // Evita capturas raras: devolvemos el Mono según la clase pedida
    given(responseSpec.bodyToMono(any(Class.class))).willAnswer(inv -> Mono.just(responseObject));
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void stubWebClientPostChainError(String expectedUri, RuntimeException toThrow) {
    given(webClient.post()).willReturn(requestBodyUriSpec);
    given(requestBodyUriSpec.uri(expectedUri)).willReturn(requestBodySpec);

    doReturn((WebClient.RequestHeadersSpec) requestHeadersSpec)
        .when(requestBodySpec)
        .bodyValue(any());

    given(requestHeadersSpec.retrieve()).willReturn(responseSpec);

    given(responseSpec.bodyToMono(any(Class.class))).willAnswer(inv -> Mono.error(toThrow));
  }

  // ---------------- helpers: reflection builders for private DTOs ----------------

  private Object buildChatResponseWithContent(String content) throws Exception {
    Object msg = newInstance("com.uade.bookybe.core.service.gateway.OpenAIClient$ChatMessage");
    setField(msg, "role", "assistant");
    setField(msg, "content", content);

    Object choice =
        newInstance("com.uade.bookybe.core.service.gateway.OpenAIClient$ChatResponse$Choice");
    setField(choice, "message", msg);

    return buildChatResponseWithChoices(List.of(choice));
  }

  private Object buildChatResponseWithChoices(List<Object> choices) throws Exception {
    Object resp = newInstance("com.uade.bookybe.core.service.gateway.OpenAIClient$ChatResponse");
    setField(resp, "choices", choices);
    // usage not required for current logic
    return resp;
  }

  private Object buildImageResponse(String url, String b64, String revisedPrompt) throws Exception {
    Object data = newInstance("com.uade.bookybe.core.service.gateway.OpenAIClient$ImageData");
    setField(data, "url", url);
    setField(data, "b64Json", b64);
    setField(data, "revisedPrompt", revisedPrompt);

    return buildImageResponseWithData(List.of(data));
  }

  private Object buildImageResponseWithData(List<Object> dataList) throws Exception {
    Object resp = newInstance("com.uade.bookybe.core.service.gateway.OpenAIClient$ImageResponse");
    setField(resp, "data", dataList);
    return resp;
  }

  private Object newInstance(String fqcn) throws Exception {
    Class<?> clazz = Class.forName(fqcn);
    Constructor<?> ctor = clazz.getDeclaredConstructor();
    ctor.setAccessible(true);
    return ctor.newInstance();
  }

  private static void setField(Object target, String fieldName, Object value) throws Exception {
    Field f = findField(target.getClass(), fieldName);
    f.setAccessible(true);
    f.set(target, value);
  }

  private static Object getField(Object target, String fieldName) throws Exception {
    Field f = findField(target.getClass(), fieldName);
    f.setAccessible(true);
    return f.get(target);
  }

  private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
    Class<?> cur = clazz;
    while (cur != null) {
      try {
        return cur.getDeclaredField(fieldName);
      } catch (NoSuchFieldException ignored) {
        cur = cur.getSuperclass();
      }
    }
    throw new NoSuchFieldException(fieldName);
  }
}
