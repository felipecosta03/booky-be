package com.uade.bookybe.router;

import com.uade.bookybe.core.model.LivekitToken;
import com.uade.bookybe.core.model.MeetingStatus;
import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.MeetingService;
import com.uade.bookybe.core.usecase.UserService;
import com.uade.bookybe.router.dto.meeting.MeetingActionResponseDto;
import com.uade.bookybe.router.dto.meeting.MeetingStatusResponseDto;
import com.uade.bookybe.router.dto.meeting.TokenRequestDto;
import com.uade.bookybe.router.dto.meeting.TokenResponseDto;
import com.uade.bookybe.router.mapper.MeetingDtoMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reading-clubs/meetings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Meetings", description = "LiveKit meeting management for reading clubs")
public class MeetingController {

    private final MeetingService meetingService;
    private final UserService userService;
    private final MeetingDtoMapper meetingDtoMapper = MeetingDtoMapper.INSTANCE;

    @Operation(summary = "Generate token for joining a meeting")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Token generated successfully"),
        @ApiResponse(responseCode = "403", description = "User is not a member of the reading club"),
        @ApiResponse(responseCode = "404", description = "Reading club not found")
    })
    @PostMapping("/token")
    public ResponseEntity<TokenResponseDto> generateToken(
            @Valid @RequestBody TokenRequestDto request,
            Authentication auth) {
        
        log.info("Generating meeting token for user {} in reading club {}", 
                auth.getName(), request.getReadingClubId());

        LivekitToken token = meetingService.generateMeetingToken(
            request.getReadingClubId(),
            auth.getName(),
            userService.getUserById(auth.getName()).get().getUsername()
        );

        TokenResponseDto response = meetingDtoMapper.toTokenResponseDto(token);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Start a meeting (moderator only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Meeting started successfully"),
        @ApiResponse(responseCode = "403", description = "Only moderators can start meetings"),
        @ApiResponse(responseCode = "404", description = "Reading club not found")
    })
    @PostMapping("/{clubId}/start")
    public ResponseEntity<MeetingActionResponseDto> startMeeting(
            @Parameter(description = "Reading club ID") @PathVariable String clubId,
            Authentication auth) {
        
        log.info("Starting meeting for reading club {} by user {}", clubId, auth.getName());

        ReadingClub updatedClub = meetingService.startMeeting(clubId, auth.getName());
        
        MeetingActionResponseDto response = meetingDtoMapper.toStartMeetingResponseDto(
            "reading-club-" + clubId,
            updatedClub.getMeetingStartedAt()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "End meeting (moderator only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Meeting ended successfully"),
        @ApiResponse(responseCode = "403", description = "Only moderators can end meetings"),
        @ApiResponse(responseCode = "404", description = "Reading club not found")
    })
    @PostMapping("/{clubId}/end")
    public ResponseEntity<MeetingActionResponseDto> endMeeting(
            @Parameter(description = "Reading club ID") @PathVariable String clubId,
            Authentication auth) {
        
        log.info("Ending meeting for reading club {} by user {}", clubId, auth.getName());

        ReadingClub updatedClub = meetingService.endMeeting(clubId, auth.getName());
        
        MeetingActionResponseDto response = meetingDtoMapper.toEndMeetingResponseDto(
            "reading-club-" + clubId,
            updatedClub.getMeetingEndedAt(),
            updatedClub.getLastMeetingDuration()
        );

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get meeting status")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Meeting status retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Reading club not found")
    })
    @GetMapping("/{clubId}/status")
    public ResponseEntity<MeetingStatusResponseDto> getMeetingStatus(
            @Parameter(description = "Reading club ID") @PathVariable String clubId) {
        
        log.info("Getting meeting status for reading club {}", clubId);

        MeetingStatus status = meetingService.getMeetingStatus(clubId);
        MeetingStatusResponseDto response = meetingDtoMapper.toMeetingStatusResponseDto(status);

        return ResponseEntity.ok(response);
    }
}
