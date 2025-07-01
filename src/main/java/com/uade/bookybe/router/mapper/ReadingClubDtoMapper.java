package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.router.dto.readingclub.ReadingClubDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = {BookDtoMapper.class, CommunityDtoMapper.class, UserDtoMapper.class})
public interface ReadingClubDtoMapper {
  ReadingClubDtoMapper INSTANCE = Mappers.getMapper(ReadingClubDtoMapper.class);

  ReadingClubDto toDto(ReadingClub model);

  ReadingClub toModel(ReadingClubDto dto);
} 