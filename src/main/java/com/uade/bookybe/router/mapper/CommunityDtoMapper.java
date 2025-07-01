package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.Community;
import com.uade.bookybe.router.dto.community.CommunityDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommunityDtoMapper {
  CommunityDtoMapper INSTANCE = Mappers.getMapper(CommunityDtoMapper.class);

  CommunityDto toDto(Community model);

  Community toModel(CommunityDto dto);
} 