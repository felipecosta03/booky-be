package com.uade.bookybe.infraestructure.mapper;

import static org.mapstruct.ReportingPolicy.IGNORE;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.infraestructure.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedSourcePolicy = IGNORE, unmappedTargetPolicy = IGNORE)
public interface UserEntityMapper {
  UserEntityMapper INSTANCE = Mappers.getMapper(UserEntityMapper.class);

  User toModel(UserEntity entity);

  UserEntity toEntity(User model);
}
