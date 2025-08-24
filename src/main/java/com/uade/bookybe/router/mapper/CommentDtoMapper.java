package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.Comment;
import com.uade.bookybe.router.dto.comment.CommentDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentDtoMapper {
  CommentDtoMapper INSTANCE = Mappers.getMapper(CommentDtoMapper.class);

  CommentDto toDto(Comment model);

  Comment toModel(CommentDto dto);
}
