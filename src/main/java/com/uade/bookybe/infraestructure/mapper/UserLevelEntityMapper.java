package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.UserLevel;
import com.uade.bookybe.infraestructure.entity.UserLevelEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserLevelEntityMapper {
  UserLevelEntityMapper INSTANCE = Mappers.getMapper(UserLevelEntityMapper.class);

  UserLevel toModel(UserLevelEntity entity);

  UserLevelEntity toEntity(UserLevel model);
}
