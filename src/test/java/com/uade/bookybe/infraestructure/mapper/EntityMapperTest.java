package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.*;
import com.uade.bookybe.infraestructure.entity.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EntityMapperTest {

    @Test
    void bookEntityMapper_ShouldMapCorrectly() {
        Book book = Book.builder()
                .id("1")
                .isbn("123")
                .title("Test")
                .author("Author")
                .build();

        BookEntity entity = BookEntityMapper.INSTANCE.toEntity(book);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
        assertEquals("Test", entity.getTitle());
    }

    @Test
    void chatEntityMapper_ShouldMapCorrectly() {
        Chat chat = Chat.builder()
                .id("1")
                .user1Id("u1")
                .user2Id("u2")
                .build();

        ChatEntity entity = ChatEntityMapper.INSTANCE.toEntity(chat);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
    }

    @Test
    void messageEntityMapper_ShouldMapCorrectly() {
        Message message = Message.builder()
                .id("1")
                .senderId("u1")
                .chatId("c1")
                .content("Hello")
                .build();

        MessageEntity entity = MessageEntityMapper.INSTANCE.toEntity(message);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
    }

    @Test
    void commentEntityMapper_ShouldMapCorrectly() {
        Comment comment = Comment.builder()
                .id("1")
                .postId("p1")
                .userId("u1")
                .body("Comment")
                .build();

        CommentEntity entity = CommentEntityMapper.INSTANCE.toEntity(comment);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
    }

    @Test
    void communityEntityMapper_ShouldMapCorrectly() {
        Community community = Community.builder()
                .id("1")
                .name("Community")
                .description("Desc")
                .adminId("u1")
                .build();

        CommunityEntity entity = CommunityEntityMapper.INSTANCE.toEntity(community);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
    }

    @Test
    void postEntityMapper_ShouldMapCorrectly() {
        Post post = Post.builder()
                .id("1")
                .userId("u1")
                .body("Body")
                .build();

        PostEntity entity = PostEntityMapper.INSTANCE.toEntity(post);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
    }

    @Test
    void readingClubEntityMapper_ShouldMapCorrectly() {
        ReadingClub club = ReadingClub.builder()
                .id("1")
                .name("Club")
                .description("Desc")
                .build();

        ReadingClubEntity entity = ReadingClubEntityMapper.INSTANCE.toEntity(club);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
    }

    @Test
    void userEntityMapper_ShouldMapCorrectly() {
        User user = User.builder()
                .id("1")
                .username("user")
                .email("user@test.com")
                .build();

        UserEntity entity = UserEntityMapper.INSTANCE.toEntity(user);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
    }

    @Test
    void achievementEntityMapper_ShouldMapCorrectly() {
        Achievement achievement = Achievement.builder()
                .id("1")
                .name("Achievement")
                .description("Desc")
                .build();

        AchievementEntity entity = AchievementEntityMapper.INSTANCE.toEntity(achievement);

        assertNotNull(entity);
        assertEquals("1", entity.getId());
    }

    @Test
    void userLevelEntityMapper_ShouldMapCorrectly() {
        UserLevel level = UserLevel.builder()
                .level(1)
                .name("Beginner")
                .build();

        UserLevelEntity entity = UserLevelEntityMapper.INSTANCE.toEntity(level);

        assertNotNull(entity);
    }
}

