package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.UserAchievement;
import com.uade.bookybe.infraestructure.entity.UserAchievementEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {AchievementEntityMapper.class})
public interface UserAchievementEntityMapper {
  UserAchievementEntityMapper INSTANCE = Mappers.getMapper(UserAchievementEntityMapper.class);

  @Mapping(source = "achievement", target = "achievement")
  UserAchievement toModel(UserAchievementEntity entity);

  @Mapping(target = "user", ignore = true)
  @Mapping(target = "achievement", ignore = true)
  UserAchievementEntity toEntity(UserAchievement model);
}
