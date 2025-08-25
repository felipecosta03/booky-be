package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.GamificationProfile;
import com.uade.bookybe.infraestructure.entity.GamificationProfileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GamificationProfileEntityMapper {
  GamificationProfileEntityMapper INSTANCE = Mappers.getMapper(GamificationProfileEntityMapper.class);

  @Mapping(target = "userLevel", ignore = true)
  @Mapping(target = "achievements", ignore = true)
  @Mapping(target = "pointsToNextLevel", ignore = true)
  GamificationProfile toModel(GamificationProfileEntity entity);

  @Mapping(target = "user", ignore = true)
  GamificationProfileEntity toEntity(GamificationProfile model);
}
