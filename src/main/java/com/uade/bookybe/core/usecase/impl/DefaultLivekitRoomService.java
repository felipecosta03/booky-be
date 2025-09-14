package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.model.LivekitRoom;
import com.uade.bookybe.core.model.MeetingStatus;
import com.uade.bookybe.core.usecase.LivekitRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultLivekitRoomService implements LivekitRoomService {

    @Override
    public LivekitRoom createRoom(String roomName, LivekitRoom.RoomOptions options) {
        try {
            log.info("Creating room: {}", roomName);
            
            // For now, return a mock room since the LiveKit API is complex
            return LivekitRoom.builder()
                .name(roomName)
                .isActive(false)
                .participantCount(0)
                .createdAt(LocalDateTime.now())
                .options(options)
                .build();

        } catch (Exception e) {
            log.error("Error creating room: " + roomName, e);
            throw new RuntimeException("Failed to create room", e);
        }
    }

    @Override
    public Optional<LivekitRoom> getRoom(String roomName) {
        try {
            log.info("Getting room: {}", roomName);
            
            // For now, return empty since we don't have a working API implementation
            return Optional.empty();

        } catch (Exception e) {
            log.error("Error getting room: " + roomName, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteRoom(String roomName) {
        try {
            log.info("Deleting room: {}", roomName);
            
            // For now, always return true
            return true;

        } catch (Exception e) {
            log.error("Error deleting room: " + roomName, e);
            return false;
        }
    }

    @Override
    public List<String> getParticipants(String roomName) {
        try {
            log.info("Getting participants for room: {}", roomName);
            
            // For now, return empty list
            return List.of();

        } catch (Exception e) {
            log.error("Error getting participants for room: " + roomName, e);
            return List.of();
        }
    }

    @Override
    public MeetingStatus getRoomStatus(String roomName) {
        try {
            log.info("Getting room status: {}", roomName);
            
            // For now, return a basic status
            return MeetingStatus.builder()
                .exists(true)
                .isActive(false)
                .participantCount(0)
                .roomName(roomName)
                .startedAt(LocalDateTime.now())
                .creationTime(System.currentTimeMillis() / 1000)
                .build();

        } catch (Exception e) {
            log.error("Error getting room status: " + roomName, e);
            return new MeetingStatus(false, false, 0, null);
        }
    }
}
