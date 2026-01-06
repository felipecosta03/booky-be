package com.uade.bookybe.router;

import com.uade.bookybe.core.model.LivekitToken;
import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.model.User;
import com.uade.bookybe.core.usecase.MeetingService;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.router.dto.meeting.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingControllerTest {

    @Mock
    private MeetingService meetingService;
    @Mock
    private UserService userService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private MeetingController controller;

    @Test
    void generateToken_Success() {
        TokenRequestDto request = new TokenRequestDto();
        request.setReadingClubId("club1");

        User user = User.builder().id("user1").username("testuser").build();
        LivekitToken token = LivekitToken.builder().token("test-token").roomName("room1").build();

        when(authentication.getName()).thenReturn("user1");
        when(userService.getUserById("user1")).thenReturn(Optional.of(user));
        when(meetingService.generateMeetingToken(anyString(), anyString(), anyString())).thenReturn(token);

        ResponseEntity<TokenResponseDto> response = controller.generateToken(request, authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(meetingService).generateMeetingToken("club1", "user1", "testuser");
    }

    @Test
    void startMeeting_Success() {
        ReadingClub club = ReadingClub.builder()
                .id("club1")
                .meetingStartedAt(LocalDateTime.now())
                .build();

        when(authentication.getName()).thenReturn("user1");
        when(meetingService.startMeeting("club1", "user1")).thenReturn(club);

        ResponseEntity<MeetingActionResponseDto> response = controller.startMeeting("club1", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(meetingService).startMeeting("club1", "user1");
    }

    @Test
    void endMeeting_Success() {
        ReadingClub club = ReadingClub.builder().id("club1").build();

        when(authentication.getName()).thenReturn("user1");
        when(meetingService.endMeeting("club1", "user1")).thenReturn(club);

        ResponseEntity<MeetingActionResponseDto> response = controller.endMeeting("club1", authentication);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(meetingService).endMeeting("club1", "user1");
    }

    @Test
    void getMeetingStatus_Success() {
        when(meetingService.getMeetingStatus("club1")).thenReturn(null);

        ResponseEntity<MeetingStatusResponseDto> response = controller.getMeetingStatus("club1");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(meetingService).getMeetingStatus("club1");
    }
}

