package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.Community;
import com.uade.bookybe.infraestructure.entity.CommunityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommunityEntityMapper {
  CommunityEntityMapper INSTANCE = Mappers.getMapper(CommunityEntityMapper.class);

  Community toModel(CommunityEntity entity);

  CommunityEntity toEntity(Community model);
} 