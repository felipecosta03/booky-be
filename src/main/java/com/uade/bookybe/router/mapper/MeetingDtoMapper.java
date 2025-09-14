package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.LivekitToken;
import com.uade.bookybe.core.model.MeetingStatus;
import com.uade.bookybe.router.dto.meeting.MeetingActionResponseDto;
import com.uade.bookybe.router.dto.meeting.MeetingStatusResponseDto;
import com.uade.bookybe.router.dto.meeting.TokenResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

@Mapper
public interface MeetingDtoMapper {
    MeetingDtoMapper INSTANCE = Mappers.getMapper(MeetingDtoMapper.class);

    @Mapping(source = "moderator", target = "isModerator")
    TokenResponseDto toTokenResponseDto(LivekitToken livekitToken);

    @Mapping(source = "active", target = "isActive")
    MeetingStatusResponseDto toMeetingStatusResponseDto(MeetingStatus meetingStatus);

    default MeetingActionResponseDto toStartMeetingResponseDto(String roomName, LocalDateTime timestamp) {
        return MeetingActionResponseDto.builder()
            .success(true)
            .roomName(roomName)
            .timestamp(timestamp)
            .message("Meeting started successfully")
            .build();
    }

    default MeetingActionResponseDto toEndMeetingResponseDto(String roomName, LocalDateTime timestamp, Long duration) {
        return MeetingActionResponseDto.builder()
            .success(true)
            .roomName(roomName)
            .timestamp(timestamp)
            .duration(duration)
            .message("Meeting ended successfully")
            .build();
    }
}
