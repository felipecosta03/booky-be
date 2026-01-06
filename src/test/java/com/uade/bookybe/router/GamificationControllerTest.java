package com.uade.bookybe.router;

import com.uade.bookybe.core.model.GamificationProfile;
import com.uade.bookybe.core.model.constant.GamificationActivity;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.router.dto.gamification.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamificationControllerTest {

    @Mock
    private GamificationService gamificationService;

    @InjectMocks
    private GamificationController controller;

    @Test
    void getUserProfile_Success() {
        GamificationProfile profile = GamificationProfile.builder()
                .userId("user1")
                .totalPoints(100)
                .build();
        when(gamificationService.getUserProfile("user1")).thenReturn(Optional.of(profile));

        ResponseEntity<GamificationProfileDto> response = controller.getUserProfile("user1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void initializeUserProfile_Success() {
        GamificationProfile profile = GamificationProfile.builder()
                .userId("user1")
                .totalPoints(0)
                .build();
        when(gamificationService.initializeUserProfile("user1")).thenReturn(Optional.of(profile));

        ResponseEntity<GamificationProfileDto> response = controller.initializeUserProfile("user1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getAllAchievements_Success() {
        when(gamificationService.getAllAchievements()).thenReturn(Arrays.asList());

        ResponseEntity<List<AchievementDto>> response = controller.getAllAchievements();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(gamificationService).getAllAchievements();
    }

    @Test
    void getUserAchievements_Success() {
        when(gamificationService.getUserAchievements("user1")).thenReturn(Arrays.asList());

        ResponseEntity<List<UserAchievementDto>> response = controller.getUserAchievements("user1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}

