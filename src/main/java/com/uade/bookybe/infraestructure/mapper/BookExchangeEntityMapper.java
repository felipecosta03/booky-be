package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.BookExchange;
import com.uade.bookybe.infraestructure.entity.BookExchangeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookExchangeEntityMapper {
  BookExchangeEntityMapper INSTANCE = Mappers.getMapper(BookExchangeEntityMapper.class);

  BookExchange toModel(BookExchangeEntity entity);

  BookExchangeEntity toEntity(BookExchange model);
}
