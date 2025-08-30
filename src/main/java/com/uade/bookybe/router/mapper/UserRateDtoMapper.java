package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.UserRate;
import com.uade.bookybe.router.dto.rate.UserRateDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserRateDtoMapper {
    UserRateDtoMapper INSTANCE = Mappers.getMapper(UserRateDtoMapper.class);

    UserRateDto toDto(UserRate model);
    UserRate toModel(UserRateDto dto);
}
