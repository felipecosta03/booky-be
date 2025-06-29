package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.model.UserSignUp;
import com.uade.bookybe.router.dto.user.UserDto;
import com.uade.bookybe.router.dto.user.UserPreviewDto;
import com.uade.bookybe.router.dto.user.UserSignUpDto;
import com.uade.bookybe.router.dto.user.UserUpdateDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserDtoMapper {
  UserDtoMapper INSTANCE = Mappers.getMapper(UserDtoMapper.class);

  UserDto toDto(User model);

  UserSignUp toModel(UserSignUpDto dto);

  User toModel(UserUpdateDto dto);

  UserPreviewDto toPreviewDto(User model);
}
