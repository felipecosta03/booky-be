package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.LivekitRoom;
import com.uade.bookybe.core.model.MeetingStatus;

import java.util.List;
import java.util.Optional;

public interface LivekitRoomService {
    LivekitRoom createRoom(String roomName, LivekitRoom.RoomOptions options);
    Optional<LivekitRoom> getRoom(String roomName);
    boolean deleteRoom(String roomName);
    List<String> getParticipants(String roomName);
    MeetingStatus getRoomStatus(String roomName);
}
