package com.uade.bookybe.router;

import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.ReadingClubService;
import com.uade.bookybe.router.dto.readingclub.*;
import com.uade.bookybe.router.mapper.ReadingClubDtoMapperWithNestedObjects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadingClubControllerTest {

    @Mock
    private ReadingClubService readingClubService;
    @Mock
    private ReadingClubDtoMapperWithNestedObjects mapper;

    @InjectMocks
    private ReadingClubController controller;

    @Test
    void getAllReadingClubs_Success() {
        when(readingClubService.getAllReadingClubs()).thenReturn(Arrays.asList());
        ResponseEntity response = controller.getAllReadingClubs();
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getReadingClubById_Success() {
        ReadingClub club = ReadingClub.builder().id("club1").name("Test Club").build();
        when(readingClubService.getReadingClubById("club1")).thenReturn(Optional.of(club));
        when(mapper.toDtoWithNestedObjects(any())).thenReturn(new ReadingClubDto());

        ResponseEntity<ReadingClubDto> response = controller.getReadingClubById("club1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getReadingClubById_NotFound() {
        when(readingClubService.getReadingClubById("club1")).thenReturn(Optional.empty());

        ResponseEntity<ReadingClubDto> response = controller.getReadingClubById("club1");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}

