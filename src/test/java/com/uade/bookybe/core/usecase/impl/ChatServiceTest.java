package com.uade.bookybe.core.usecase.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.never;

import com.uade.bookybe.core.model.Chat;
import com.uade.bookybe.core.model.Message;
import com.uade.bookybe.core.port.ImageStoragePort;
import com.uade.bookybe.infraestructure.entity.ChatEntity;
import com.uade.bookybe.infraestructure.entity.MessageEntity;
import com.uade.bookybe.infraestructure.repository.ChatRepository;
import com.uade.bookybe.infraestructure.repository.MessageRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

  @Mock private ChatRepository chatRepository;
  @Mock private MessageRepository messageRepository;
  @Mock private ImageStoragePort imageStoragePort;

  @InjectMocks private ChatServiceImpl sut;

  // ---------------- createOrGetChat ----------------

  @Test
  void createOrGetChat_deberiaRetornarChatExistente_siYaExiste() {
    // given
    ChatEntity existing = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findByUsers("u1", "u2")).willReturn(Optional.of(existing));

    // when
    Optional<Chat> result = sut.createOrGetChat("u1", "u2");

    // then
    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
    then(chatRepository).should(never()).save(any(ChatEntity.class));
  }

  @Test
  void createOrGetChat_deberiaCrearNuevoChat_yGuardar_siNoExiste() {
    // given
    given(chatRepository.findByUsers("u1", "u2")).willReturn(Optional.empty());
    given(chatRepository.save(any(ChatEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ChatEntity.class));

    ArgumentCaptor<ChatEntity> captor = ArgumentCaptor.forClass(ChatEntity.class);

    // when
    Optional<Chat> result = sut.createOrGetChat("u1", "u2");

    // then
    assertTrue(result.isPresent());
    then(chatRepository).should().save(captor.capture());
    ChatEntity saved = captor.getValue();
    assertNotNull(saved.getId());
    assertEquals("u1", saved.getUser1Id());
    assertEquals("u2", saved.getUser2Id());
    assertNotNull(saved.getDateCreated());
    assertNotNull(saved.getDateUpdated());
  }

  // ---------------- sendMessage ----------------

  @Test
  void sendMessage_deberiaRetornarEmpty_siChatNoExiste() {
    // given
    given(chatRepository.findById("c1")).willReturn(Optional.empty());

    // when
    Optional<Message> result = sut.sendMessage("c1", "u1", "hola", null);

    // then
    assertTrue(result.isEmpty());
    then(messageRepository).shouldHaveNoInteractions();
    then(imageStoragePort).shouldHaveNoInteractions();
  }

  @Test
  void sendMessage_deberiaRetornarEmpty_siSenderNoPerteneceAlChat() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findById("c1")).willReturn(Optional.of(chat));

    // when
    Optional<Message> result = sut.sendMessage("c1", "u3", "hola", null);

    // then
    assertTrue(result.isEmpty());
    then(messageRepository).shouldHaveNoInteractions();
    then(imageStoragePort).shouldHaveNoInteractions();
    then(chatRepository).should(never()).save(any(ChatEntity.class));
  }

  @Test
  void sendMessage_deberiaGuardarMensaje_yActualizarChat_siValido_sinImagen() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findById("c1")).willReturn(Optional.of(chat));

    given(messageRepository.save(any(MessageEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, MessageEntity.class));

    given(chatRepository.save(any(ChatEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ChatEntity.class));

    ArgumentCaptor<MessageEntity> msgCaptor = ArgumentCaptor.forClass(MessageEntity.class);
    ArgumentCaptor<ChatEntity> chatCaptor = ArgumentCaptor.forClass(ChatEntity.class);

    // when
    Optional<Message> result = sut.sendMessage("c1", "u1", "hola", null);

    // then
    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getChatId());
    assertEquals("u1", result.get().getSenderId());
    assertEquals("hola", result.get().getContent());
    assertFalse(result.get().isRead());

    then(messageRepository).should().save(msgCaptor.capture());
    MessageEntity savedMsg = msgCaptor.getValue();
    assertNotNull(savedMsg.getId());
    assertEquals("c1", savedMsg.getChatId());
    assertEquals("u1", savedMsg.getSenderId());
    assertEquals("hola", savedMsg.getContent());
    assertNotNull(savedMsg.getDateSent());
    assertFalse(savedMsg.isRead());
    assertNull(savedMsg.getImage());

    then(chatRepository).should().save(chatCaptor.capture());
    ChatEntity savedChat = chatCaptor.getValue();
    assertEquals("c1", savedChat.getId());
    assertNotNull(savedChat.getDateUpdated());

    then(imageStoragePort).shouldHaveNoInteractions();
  }

  @Test
  void sendMessage_deberiaSubirImagen_yPersistirURL_siImagenBase64NoNull() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findById("c1")).willReturn(Optional.of(chat));

    given(imageStoragePort.uploadImage("b64", "booky/messages")).willReturn(Optional.of("img-url"));

    given(messageRepository.save(any(MessageEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, MessageEntity.class));

    given(chatRepository.save(any(ChatEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ChatEntity.class));

    ArgumentCaptor<MessageEntity> msgCaptor = ArgumentCaptor.forClass(MessageEntity.class);

    // when
    Optional<Message> result = sut.sendMessage("c1", "u2", "hola", "b64");

    // then
    assertTrue(result.isPresent());
    assertEquals("img-url", result.get().getImage());

    then(imageStoragePort).should().uploadImage("b64", "booky/messages");
    then(messageRepository).should().save(msgCaptor.capture());
    assertEquals("img-url", msgCaptor.getValue().getImage());
  }

  @Test
  void sendMessage_deberiaNoSetearImagen_siUploadDevuelveEmpty() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findById("c1")).willReturn(Optional.of(chat));

    given(imageStoragePort.uploadImage("b64", "booky/messages")).willReturn(Optional.empty());

    given(messageRepository.save(any(MessageEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, MessageEntity.class));

    given(chatRepository.save(any(ChatEntity.class)))
        .willAnswer(inv -> inv.getArgument(0, ChatEntity.class));

    ArgumentCaptor<MessageEntity> msgCaptor = ArgumentCaptor.forClass(MessageEntity.class);

    // when
    Optional<Message> result = sut.sendMessage("c1", "u1", "hola", "b64");

    // then
    assertTrue(result.isPresent());
    assertNull(result.get().getImage());

    then(messageRepository).should().save(msgCaptor.capture());
    assertNull(msgCaptor.getValue().getImage());
  }

  // ---------------- getChatMessages ----------------

  @Test
  void getChatMessages_deberiaRetornarListaVacia_siChatNoExiste() {
    // given
    given(chatRepository.findById("c1")).willReturn(Optional.empty());

    // when
    List<Message> result = sut.getChatMessages("c1", "u1");

    // then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    then(messageRepository).shouldHaveNoInteractions();
  }

  @Test
  void getChatMessages_deberiaRetornarListaVacia_siUserNoPertenece() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findById("c1")).willReturn(Optional.of(chat));

    // when
    List<Message> result = sut.getChatMessages("c1", "u9");

    // then
    assertNotNull(result);
    assertTrue(result.isEmpty());
    then(messageRepository).shouldHaveNoInteractions();
  }

  @Test
  void getChatMessages_deberiaMapearMensajes_siUserPertenece() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findById("c1")).willReturn(Optional.of(chat));

    MessageEntity m1 =
        MessageEntity.builder()
            .id("m1")
            .chatId("c1")
            .senderId("u1")
            .content("a")
            .read(false)
            .build();
    MessageEntity m2 =
        MessageEntity.builder()
            .id("m2")
            .chatId("c1")
            .senderId("u2")
            .content("b")
            .read(true)
            .build();

    given(messageRepository.findByChatIdWithSenderOrderByDateSentAsc("c1"))
        .willReturn(List.of(m1, m2));

    // when
    List<Message> result = sut.getChatMessages("c1", "u1");

    // then
    assertEquals(2, result.size());
    assertEquals("m1", result.get(0).getId());
    assertEquals("m2", result.get(1).getId());
    then(messageRepository).should().findByChatIdWithSenderOrderByDateSentAsc("c1");
  }

  // ---------------- getUserChats ----------------

  @Test
  void getUserChats_deberiaMapearChats_ySetearLastMessage_siExiste() {
    // given
    ChatEntity c1 = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    ChatEntity c2 = ChatEntity.builder().id("c2").user1Id("u1").user2Id("u3").build();

    given(chatRepository.findByUserIdWithUsersOrderByDateUpdatedDesc("u1"))
        .willReturn(List.of(c1, c2));

    MessageEntity last1 =
        MessageEntity.builder().id("m1").chatId("c1").senderId("u2").content("hey").build();
    given(messageRepository.findLastMessageByChatId("c1")).willReturn(Optional.of(last1));
    given(messageRepository.findLastMessageByChatId("c2")).willReturn(Optional.empty());

    // when
    List<Chat> result = sut.getUserChats("u1");

    // then
    assertEquals(2, result.size());
    Chat chat1 = result.stream().filter(c -> "c1".equals(c.getId())).findFirst().orElseThrow();
    assertNotNull(chat1.getLastMessage());
    assertEquals("m1", chat1.getLastMessage().getId());

    Chat chat2 = result.stream().filter(c -> "c2".equals(c.getId())).findFirst().orElseThrow();
    assertNull(chat2.getLastMessage());

    then(messageRepository).should().findLastMessageByChatId("c1");
    then(messageRepository).should().findLastMessageByChatId("c2");
  }

  // ---------------- getChatById ----------------

  @Test
  void getChatById_deberiaRetornarEmpty_siNoExiste() {
    // given
    given(chatRepository.findByIdWithUsers("c1")).willReturn(Optional.empty());

    // when
    Optional<Chat> result = sut.getChatById("c1", "u1");

    // then
    assertTrue(result.isEmpty());
  }

  @Test
  void getChatById_deberiaRetornarEmpty_siUserNoPertenece() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findByIdWithUsers("c1")).willReturn(Optional.of(chat));

    // when
    Optional<Chat> result = sut.getChatById("c1", "u9");

    // then
    assertTrue(result.isEmpty());
  }

  @Test
  void getChatById_deberiaRetornarChat_siUserPertenece() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findByIdWithUsers("c1")).willReturn(Optional.of(chat));

    // when
    Optional<Chat> result = sut.getChatById("c1", "u2");

    // then
    assertTrue(result.isPresent());
    assertEquals("c1", result.get().getId());
  }

  // ---------------- markMessagesAsRead ----------------

  @Test
  void markMessagesAsRead_noHaceNada_siChatNoExiste() {
    // given
    given(chatRepository.findById("c1")).willReturn(Optional.empty());

    // when
    sut.markMessagesAsRead("c1", "u1");

    // then
    then(messageRepository).shouldHaveNoInteractions();
  }

  @Test
  void markMessagesAsRead_noHaceNada_siUserNoPertenece() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findById("c1")).willReturn(Optional.of(chat));

    // when
    sut.markMessagesAsRead("c1", "u9");

    // then
    then(messageRepository).shouldHaveNoInteractions();
  }

  @Test
  void markMessagesAsRead_deberiaMarcarComoRead_soloMensajesDelOtro_yNoLeidos() {
    // given
    ChatEntity chat = ChatEntity.builder().id("c1").user1Id("u1").user2Id("u2").build();
    given(chatRepository.findById("c1")).willReturn(Optional.of(chat));

    MessageEntity m1 =
        MessageEntity.builder()
            .id("m1")
            .chatId("c1")
            .senderId("u2")
            .read(false)
            .build(); // debe marcar
    MessageEntity m2 =
        MessageEntity.builder().id("m2").chatId("c1").senderId("u2").read(true).build(); // ya leído
    MessageEntity m3 =
        MessageEntity.builder().id("m3").chatId("c1").senderId("u1").read(false).build(); // propio

    List<MessageEntity> all = new ArrayList<>(List.of(m1, m2, m3));

    given(messageRepository.findByChatIdOrderByDateSentAsc("c1")).willReturn(all);
    given(messageRepository.saveAll(anyList())).willAnswer(inv -> inv.getArgument(0));

    ArgumentCaptor<List<MessageEntity>> captor = ArgumentCaptor.forClass(List.class);

    // when
    sut.markMessagesAsRead("c1", "u1");

    // then
    then(messageRepository).should().saveAll(captor.capture());
    List<MessageEntity> saved = captor.getValue();

    assertEquals(1, saved.size());
    assertEquals("m1", saved.get(0).getId());
    assertTrue(saved.get(0).isRead());
  }
}
