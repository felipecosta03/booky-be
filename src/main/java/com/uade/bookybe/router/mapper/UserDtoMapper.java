package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.router.dto.user.UserDto;
import com.uade.bookybe.router.dto.user.UserSignUpDto;
import com.uade.bookybe.router.dto.user.UserUpdateDto;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserDtoMapper {
  UserDtoMapper INSTANCE = Mappers.getMapper(UserDtoMapper.class);

  User toModel(UserDto dto);

  UserDto toDto(User model);

  UserSignUp toModel(UserSignUpDto dto);

  User toModel(UserUpdateDto dto);

  UserPreviewDto toPreviewDto(User model);
}
