package com.uade.bookybe.router.mapper;

import static org.mapstruct.factory.Mappers.getMapper;

import com.uade.bookybe.core.model.Message;
import com.uade.bookybe.router.dto.chat.MessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(uses = {UserDtoMapper.class})
public interface MessageDtoMapper {

  MessageDtoMapper INSTANCE = getMapper(MessageDtoMapper.class);

  MessageDto toDto(Message model);

  @Mapping(target = "sender.address", ignore = true)
  @Mapping(target = "sender.dateCreated", ignore = true)
  @Mapping(target = "sender.description", ignore = true)
  @Mapping(target = "sender.email", ignore = true)
  @Mapping(target = "sender.password", ignore = true)
  Message toModel(MessageDto dto);
}
