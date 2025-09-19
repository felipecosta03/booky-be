package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.infraestructure.mapper.BookEntityMapper;
import com.uade.bookybe.infraestructure.mapper.UserEntityMapper;
import com.uade.bookybe.infraestructure.repository.BookRepository;
import com.uade.bookybe.infraestructure.repository.UserRepository;
import com.uade.bookybe.router.dto.readingclub.ReadingClubDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReadingClubDtoMapperWithNestedObjects {

  private final BookRepository bookRepository;
  private final UserRepository userRepository;

  public ReadingClubDto toDtoWithNestedObjects(ReadingClub model) {
    if (model == null) {
      return null;
    }

    // Mapeo básico usando el mapper simple
    ReadingClubDto dto = ReadingClubDtoMapper.INSTANCE.toDto(model);

    // Enriquecer con objetos anidados
    try {
      // Cargar book si bookId existe - usando método con fetch eager para categories
      if (model.getBookId() != null) {
        bookRepository
            .findByIdWithCategories(model.getBookId())
            .map(BookEntityMapper.INSTANCE::toModel)
            .map(BookDtoMapper.INSTANCE::toDto)
            .ifPresent(dto::setBook);
      }

      // Cargar moderator si moderatorId existe
      if (model.getModeratorId() != null) {
        userRepository
            .findById(model.getModeratorId())
            .map(UserEntityMapper.INSTANCE::toModel)
            .map(UserDtoMapper.INSTANCE::toPreviewDto)
            .ifPresent(dto::setModerator);
      }

    } catch (Exception e) {
      log.warn(
          "Error enriching ReadingClub DTO with nested objects for ID {}: {}",
          model.getId(),
          e.getMessage());
    }

    return dto;
  }
}
