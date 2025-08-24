package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.Community;
import com.uade.bookybe.router.dto.community.CommunityDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = UserDtoMapper.class)
public interface CommunityDtoMapper {
  CommunityDtoMapper INSTANCE = Mappers.getMapper(CommunityDtoMapper.class);

  @Mapping(target = "memberCount", ignore = true)
  @Mapping(target = "admin", source = "admin")
  CommunityDto toDto(Community model);

  Community toModel(CommunityDto dto);
} 