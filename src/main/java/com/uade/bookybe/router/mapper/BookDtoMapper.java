package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.Book;
import com.uade.bookybe.core.model.UserBook;
import com.uade.bookybe.router.dto.book.BookDto;
import com.uade.bookybe.router.dto.book.UserBookDto;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BookDtoMapper {
  BookDtoMapper INSTANCE = Mappers.getMapper(BookDtoMapper.class);

  BookDto toDto(Book model);

  Book toModel(BookDto dto);

  UserBookDto toUserBookDto(UserBook userBook);
}
