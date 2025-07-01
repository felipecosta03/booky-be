package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.Post;
import com.uade.bookybe.router.dto.post.PostDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PostDtoMapper {
  PostDtoMapper INSTANCE = Mappers.getMapper(PostDtoMapper.class);

  PostDto toDto(Post model);

  Post toModel(PostDto dto);
} 