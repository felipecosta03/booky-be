package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.Post;
import com.uade.bookybe.infraestructure.entity.PostEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostEntityMapper {
  PostEntityMapper INSTANCE = Mappers.getMapper(PostEntityMapper.class);

  Post toModel(PostEntity entity);

  PostEntity toEntity(Post model);
} 