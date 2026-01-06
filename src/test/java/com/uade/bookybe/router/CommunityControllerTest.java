package com.uade.bookybe.router;

import com.uade.bookybe.core.model.Community;
import com.uade.bookybe.core.usecase.CommunityService;
import com.uade.bookybe.router.dto.community.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CommunityControllerTest {

    @Mock
    private CommunityService communityService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CommunityController communityController;

    private Community testCommunity;

    @BeforeEach
    void setUp() {
        testCommunity = Community.builder()
                .id("community123")
                .name("Test Community")
                .description("Test Description")
                .adminId("user123")
                .build();

        when(authentication.getName()).thenReturn("user123");
    }

    @Test
    void createCommunity_Success() {
        CreateCommunityDto dto = new CreateCommunityDto();
        dto.setName("Test Community");
        dto.setDescription("Test Description");

        when(communityService.createCommunity("user123", "Test Community", "Test Description"))
                .thenReturn(Optional.of(testCommunity));

        ResponseEntity<CommunityDto> response = communityController.createCommunity(dto, authentication);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void createCommunity_BadRequest() {
        CreateCommunityDto dto = new CreateCommunityDto();
        dto.setName("Test");

        when(communityService.createCommunity(anyString(), anyString(), any()))
                .thenReturn(Optional.empty());

        ResponseEntity<CommunityDto> response = communityController.createCommunity(dto, authentication);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void getAllCommunities_Success() {
        when(communityService.getAllCommunities()).thenReturn(Arrays.asList(testCommunity));

        ResponseEntity<List<CommunityDto>> response = communityController.getAllCommunities();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getCommunityById_Success() {
        when(communityService.getCommunityById("community123")).thenReturn(Optional.of(testCommunity));

        ResponseEntity<CommunityDto> response = communityController.getCommunityById("community123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getCommunityById_NotFound() {
        when(communityService.getCommunityById("community123")).thenReturn(Optional.empty());

        ResponseEntity<CommunityDto> response = communityController.getCommunityById("community123");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void getUserCommunities_Success() {
        when(communityService.getUserCommunities("user123")).thenReturn(Arrays.asList(testCommunity));

        ResponseEntity<List<CommunityDto>> response = communityController.getUserCommunities("user123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

