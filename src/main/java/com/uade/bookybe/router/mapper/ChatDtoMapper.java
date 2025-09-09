package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.Chat;
import com.uade.bookybe.router.dto.chat.ChatDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UserDtoMapper.class, MessageDtoMapper.class})
public interface ChatDtoMapper {

  ChatDtoMapper INSTANCE = Mappers.getMapper(ChatDtoMapper.class);

  @Mapping(target = "unreadCount", ignore = true)
  ChatDto toDto(Chat model);

  @Mapping(target = "messages", ignore = true)
  @Mapping(target = "lastMessage", ignore = true)
  @Mapping(target = "user1.address", ignore = true)
  @Mapping(target = "user1.dateCreated", ignore = true)
  @Mapping(target = "user1.description", ignore = true)
  @Mapping(target = "user1.email", ignore = true)
  @Mapping(target = "user1.password", ignore = true)
  @Mapping(target = "user2.address", ignore = true)
  @Mapping(target = "user2.dateCreated", ignore = true)
  @Mapping(target = "user2.description", ignore = true)
  @Mapping(target = "user2.email", ignore = true)
  @Mapping(target = "user2.password", ignore = true)
  Chat toModel(ChatDto dto);
}
