package com.uade.bookybe.infraestructure.mapper;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.infraestructure.entity.BookEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookEntityMapper {
  BookEntityMapper INSTANCE = Mappers.getMapper(BookEntityMapper.class);

  Book toModel(BookEntity entity);

  BookEntity toEntity(Book model);
}
