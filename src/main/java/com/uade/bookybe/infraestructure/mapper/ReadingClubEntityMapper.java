package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.infraestructure.entity.ReadingClubEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReadingClubEntityMapper {
  ReadingClubEntityMapper INSTANCE = Mappers.getMapper(ReadingClubEntityMapper.class);

  ReadingClub toModel(ReadingClubEntity entity);

  ReadingClubEntity toEntity(ReadingClub model);
}
