package com.uade.bookybe.infraestructure.mapper;

import static org.mapstruct.factory.Mappers.getMapper;

import com.uade.bookybe.core.model.Message;
import com.uade.bookybe.infraestructure.entity.MessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {UserEntityMapper.class})
public interface MessageEntityMapper {

  MessageEntityMapper INSTANCE = getMapper(MessageEntityMapper.class);

  Message toModel(MessageEntity entity);

  @Mapping(target = "chat", ignore = true)
  @Mapping(target = "sender", ignore = true)
  MessageEntity toEntity(Message model);
}
