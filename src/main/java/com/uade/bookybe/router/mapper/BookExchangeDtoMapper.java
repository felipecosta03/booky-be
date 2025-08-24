package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.BookExchange;
import com.uade.bookybe.router.dto.exchange.BookExchangeDto;
import com.uade.bookybe.router.dto.exchange.CounterOfferDto;
import com.uade.bookybe.router.dto.exchange.CreateBookExchangeDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookExchangeDtoMapper {
  BookExchangeDtoMapper INSTANCE = Mappers.getMapper(BookExchangeDtoMapper.class);

  BookExchangeDto toDto(BookExchange model);

  BookExchange toModel(CreateBookExchangeDto dto);

  BookExchange toModel(CounterOfferDto dto);
} 