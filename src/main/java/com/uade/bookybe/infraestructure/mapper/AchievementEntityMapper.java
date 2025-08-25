package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.Achievement;
import com.uade.bookybe.infraestructure.entity.AchievementEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AchievementEntityMapper {
  AchievementEntityMapper INSTANCE = Mappers.getMapper(AchievementEntityMapper.class);

  Achievement toModel(AchievementEntity entity);

  AchievementEntity toEntity(Achievement model);
}
