package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.*;
import com.uade.bookybe.router.dto.gamification.*;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface GamificationDtoMapper {
  GamificationDtoMapper INSTANCE = Mappers.getMapper(GamificationDtoMapper.class);

  // GamificationProfile mappings
  GamificationProfileDto toDto(GamificationProfile model);
  GamificationProfile toModel(GamificationProfileDto dto);

  // Achievement mappings
  AchievementDto toDto(Achievement model);
  Achievement toModel(AchievementDto dto);

  // UserLevel mappings
  UserLevelDto toDto(UserLevel model);
  UserLevel toModel(UserLevelDto dto);

  // UserAchievement mappings
  UserAchievementDto toDto(UserAchievement model);
  UserAchievement toModel(UserAchievementDto dto);
}
