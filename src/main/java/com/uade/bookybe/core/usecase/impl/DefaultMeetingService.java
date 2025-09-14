package com.uade.bookybe.core.usecase.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.exception.UnauthorizedException;
import com.uade.bookybe.core.model.LivekitRoom;
import com.uade.bookybe.core.model.LivekitToken;
import com.uade.bookybe.core.model.MeetingStatus;
import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.LivekitRoomService;
import com.uade.bookybe.core.usecase.LivekitTokenService;
import com.uade.bookybe.core.usecase.MeetingService;
import com.uade.bookybe.core.usecase.ReadingClubService;
import com.uade.bookybe.infraestructure.entity.ReadingClubEntity;
import com.uade.bookybe.infraestructure.mapper.ReadingClubEntityMapper;
import com.uade.bookybe.infraestructure.repository.ReadingClubRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultMeetingService implements MeetingService {

    private final LivekitTokenService livekitTokenService;
    private final LivekitRoomService livekitRoomService;
    private final ReadingClubService readingClubService;
    private final ReadingClubRepository readingClubRepository;
    private final ObjectMapper objectMapper;

    @Override
    public LivekitToken generateMeetingToken(String readingClubId, String userId, String participantName) {
        log.info("Generating meeting token for user {} in reading club {}", userId, readingClubId);

        // Verify reading club exists
        Optional<ReadingClub> clubOpt = readingClubService.getReadingClubById(readingClubId);
        if (clubOpt.isEmpty()) {
            throw new NotFoundException("Reading club not found: " + readingClubId);
        }

        ReadingClub club = clubOpt.get();

        // Verify user is member of the reading club
        if (!readingClubService.isUserMember(readingClubId, userId)) {
            throw new UnauthorizedException("User is not a member of this reading club");
        }

        // Check if user is moderator
        boolean isModerator = club.getModeratorId().equals(userId);

        // Generate room name
        String roomName = "reading-club-" + readingClubId;

        // Create token with appropriate permissions
        LivekitToken.TokenPermissions permissions = new LivekitToken.TokenPermissions(isModerator);

        LivekitToken token = livekitTokenService.createToken(
            roomName,
            participantName,
            userId,
            permissions
        );

        log.info("Generated meeting token for user {} (moderator: {})", userId, isModerator);
        return token;
    }

    @Override
    @Transactional
    public ReadingClub startMeeting(String readingClubId, String moderatorId) {
        log.info("Starting meeting for reading club {} by moderator {}", readingClubId, moderatorId);

        // Verify reading club exists and user is moderator
        Optional<ReadingClub> clubOpt = readingClubService.getReadingClubById(readingClubId);
        if (clubOpt.isEmpty()) {
            throw new NotFoundException("Reading club not found: " + readingClubId);
        }

        ReadingClub club = clubOpt.get();
        if (!club.getModeratorId().equals(moderatorId)) {
            throw new UnauthorizedException("Only moderators can start meetings");
        }

        String roomName = "reading-club-" + readingClubId;

        try {
            // Create room with metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("clubId", readingClubId);
            metadata.put("clubName", club.getName());
            metadata.put("moderatorId", moderatorId);
            metadata.put("startedAt", LocalDateTime.now().toString());

            LivekitRoom.RoomOptions options = LivekitRoom.RoomOptions.builder()
                .emptyTimeout(600) // 10 minutes empty timeout
                .maxParticipants(50) // max participants
                .metadata(objectMapper.writeValueAsString(metadata))
                .build();

            livekitRoomService.createRoom(roomName, options);

            // Update database
            Optional<ReadingClubEntity> entityOpt = readingClubRepository.findById(readingClubId);
            if (entityOpt.isPresent()) {
                ReadingClubEntity entity = entityOpt.get();
                entity.setMeetingActive(true);
                entity.setMeetingStartedAt(LocalDateTime.now());
                readingClubRepository.save(entity);

                // Convert back to model
                ReadingClub updatedClub = ReadingClubEntityMapper.INSTANCE.toModel(entity);
                updatedClub.setMemberCount(readingClubService.getMemberCount(readingClubId));

                log.info("Meeting started successfully for reading club {}", readingClubId);
                return updatedClub;
            }

            throw new RuntimeException("Failed to update reading club after starting meeting");

        } catch (Exception e) {
            log.error("Error starting meeting for reading club {}", readingClubId, e);
            throw new RuntimeException("Failed to start meeting", e);
        }
    }

    @Override
    @Transactional
    public ReadingClub endMeeting(String readingClubId, String moderatorId) {
        log.info("Ending meeting for reading club {} by moderator {}", readingClubId, moderatorId);

        // Verify reading club exists and user is moderator
        Optional<ReadingClub> clubOpt = readingClubService.getReadingClubById(readingClubId);
        if (clubOpt.isEmpty()) {
            throw new NotFoundException("Reading club not found: " + readingClubId);
        }

        ReadingClub club = clubOpt.get();
        if (!club.getModeratorId().equals(moderatorId)) {
            throw new UnauthorizedException("Only moderators can end meetings");
        }

        String roomName = "reading-club-" + readingClubId;

        try {
            // Delete room (disconnects all participants)
            livekitRoomService.deleteRoom(roomName);

            // Update database
            Optional<ReadingClubEntity> entityOpt = readingClubRepository.findById(readingClubId);
            if (entityOpt.isPresent()) {
                ReadingClubEntity entity = entityOpt.get();
                LocalDateTime endTime = LocalDateTime.now();
                LocalDateTime startTime = entity.getMeetingStartedAt();
                long duration = startTime != null ? 
                    java.time.Duration.between(startTime, endTime).getSeconds() : 0;

                entity.setMeetingActive(false);
                entity.setMeetingEndedAt(endTime);
                entity.setLastMeetingDuration(duration);
                readingClubRepository.save(entity);

                // Convert back to model
                ReadingClub updatedClub = ReadingClubEntityMapper.INSTANCE.toModel(entity);
                updatedClub.setMemberCount(readingClubService.getMemberCount(readingClubId));

                log.info("Meeting ended successfully for reading club {} (duration: {} seconds)", 
                        readingClubId, duration);
                return updatedClub;
            }

            throw new RuntimeException("Failed to update reading club after ending meeting");

        } catch (Exception e) {
            log.error("Error ending meeting for reading club {}", readingClubId, e);
            throw new RuntimeException("Failed to end meeting", e);
        }
    }

    @Override
    public MeetingStatus getMeetingStatus(String readingClubId) {
        log.info("Getting meeting status for reading club {}", readingClubId);

        String roomName = "reading-club-" + readingClubId;

        // Get room status from LiveKit
        MeetingStatus status = livekitRoomService.getRoomStatus(roomName);

        // Get database info
        Optional<ReadingClub> clubOpt = readingClubService.getReadingClubById(readingClubId);
        if (clubOpt.isPresent()) {
            ReadingClub club = clubOpt.get();
            status.setStartedAt(club.getMeetingStartedAt());
        }

        return status;
    }

    @Override
    public boolean isMemberOfReadingClub(String userId, String readingClubId) {
        return readingClubService.isUserMember(readingClubId, userId);
    }

    @Override
    public Optional<ReadingClub> getReadingClub(String readingClubId) {
        return readingClubService.getReadingClubById(readingClubId);
    }
}
