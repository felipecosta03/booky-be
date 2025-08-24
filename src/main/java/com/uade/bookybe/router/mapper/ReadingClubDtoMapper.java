package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.router.dto.readingclub.ReadingClubDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReadingClubDtoMapper {
  ReadingClubDtoMapper INSTANCE = Mappers.getMapper(ReadingClubDtoMapper.class);

  @Mapping(target = "book", ignore = true)
  @Mapping(target = "community", ignore = true)
  @Mapping(target = "moderator", ignore = true)
  ReadingClubDto toDto(ReadingClub model);

  @Mapping(target = "memberCount", ignore = true)
  ReadingClub toModel(ReadingClubDto dto);
} 