package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.Chat;
import com.uade.bookybe.infraestructure.entity.ChatEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.factory.Mappers.getMapper;

@Mapper(uses = {UserEntityMapper.class, MessageEntityMapper.class})
public interface ChatEntityMapper {

  ChatEntityMapper INSTANCE = getMapper(ChatEntityMapper.class);

  @Mapping(target = "messages", ignore = true)
  @Mapping(target = "lastMessage", ignore = true)
  Chat toModel(ChatEntity entity);

  @Mapping(target = "messages", ignore = true)
  @Mapping(target = "user1", ignore = true)
  @Mapping(target = "user2", ignore = true)
  ChatEntity toEntity(Chat model);
}
