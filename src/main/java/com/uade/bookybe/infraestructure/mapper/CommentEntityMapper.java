package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.Comment;
import com.uade.bookybe.infraestructure.entity.CommentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentEntityMapper {
  CommentEntityMapper INSTANCE = Mappers.getMapper(CommentEntityMapper.class);

  Comment toModel(CommentEntity entity);

  CommentEntity toEntity(Comment model);
}
