package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.Address;
import com.uade.bookybe.router.dto.user.AddressDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface AddressDtoMapper {
  AddressDtoMapper INSTANCE = Mappers.getMapper(AddressDtoMapper.class);

  Address toModel(AddressDto dto);

  AddressDto toDto(Address model);
} 