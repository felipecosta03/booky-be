package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.UserRate;
import com.uade.bookybe.infraestructure.entity.UserRateEntity;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserRateEntityMapper {
    UserRateEntityMapper INSTANCE = Mappers.getMapper(UserRateEntityMapper.class);

    UserRate toModel(UserRateEntity entity);
    UserRateEntity toEntity(UserRate model);
}
