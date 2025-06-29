package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.UserBook;
import com.uade.bookybe.infraestructure.entity.UserBookEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserBookEntityMapper {
  UserBookEntityMapper INSTANCE = Mappers.getMapper(UserBookEntityMapper.class);

  UserBook toModel(UserBookEntity entity);

  UserBookEntity toEntity(UserBook model);
}
