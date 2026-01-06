package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.*;
import com.uade.bookybe.router.dto.book.*;
import com.uade.bookybe.router.dto.chat.*;
import com.uade.bookybe.router.dto.comment.*;
import com.uade.bookybe.router.dto.community.*;
import com.uade.bookybe.router.dto.exchange.*;
import com.uade.bookybe.router.dto.gamification.*;
import com.uade.bookybe.router.dto.post.*;
import com.uade.bookybe.router.dto.rate.*;
import com.uade.bookybe.router.dto.readingclub.*;
import com.uade.bookybe.router.dto.user.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class MapperTest {

    @Test
    void bookDtoMapper_ShouldMapCorrectly() {
        Book book = Book.builder()
                .id("1")
                .isbn("123")
                .title("Test")
                .author("Author")
                .build();

        BookDto dto = BookDtoMapper.INSTANCE.toDto(book);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
        assertEquals("Test", dto.getTitle());
    }

    @Test
    void chatDtoMapper_ShouldMapCorrectly() {
        Chat chat = Chat.builder()
                .id("1")
                .user1Id("u1")
                .user2Id("u2")
                .build();

        ChatDto dto = ChatDtoMapper.INSTANCE.toDto(chat);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
    }

    @Test
    void messageDtoMapper_ShouldMapCorrectly() {
        Message message = Message.builder()
                .id("1")
                .senderId("u1")
                .chatId("c1")
                .content("Hello")
                .build();

        MessageDto dto = MessageDtoMapper.INSTANCE.toDto(message);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
        assertEquals("Hello", dto.getContent());
    }

    @Test
    void commentDtoMapper_ShouldMapCorrectly() {
        Comment comment = Comment.builder()
                .id("1")
                .postId("p1")
                .userId("u1")
                .body("Comment")
                .build();

        CommentDto dto = CommentDtoMapper.INSTANCE.toDto(comment);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
    }

    @Test
    void communityDtoMapper_ShouldMapCorrectly() {
        Community community = Community.builder()
                .id("1")
                .name("Community")
                .description("Desc")
                .adminId("u1")
                .build();

        CommunityDto dto = CommunityDtoMapper.INSTANCE.toDto(community);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
    }

    @Test
    void postDtoMapper_ShouldMapCorrectly() {
        Post post = Post.builder()
                .id("1")
                .userId("u1")
                .body("Body")
                .build();

        PostDto dto = PostDtoMapper.INSTANCE.toDto(post);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
    }

    @Test
    void readingClubDtoMapper_ShouldMapCorrectly() {
        ReadingClub club = ReadingClub.builder()
                .id("1")
                .name("Club")
                .description("Desc")
                .build();

        ReadingClubDto dto = ReadingClubDtoMapper.INSTANCE.toDto(club);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
    }

    @Test
    void userDtoMapper_ShouldMapCorrectly() {
        User user = User.builder()
                .id("1")
                .username("user")
                .email("user@test.com")
                .build();

        UserDto dto = UserDtoMapper.INSTANCE.toDto(user);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
        assertEquals("user", dto.getUsername());
    }

    @Test
    void userPreviewDtoMapper_ShouldMapCorrectly() {
        User user = User.builder()
                .id("1")
                .username("user")
                .email("user@test.com")
                .build();

        UserPreviewDto dto = UserDtoMapper.INSTANCE.toPreviewDto(user);

        assertNotNull(dto);
        assertEquals("1", dto.getId());
    }
}

