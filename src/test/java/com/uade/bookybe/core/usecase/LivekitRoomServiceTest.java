package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.model.LivekitRoom;
import com.uade.bookybe.core.model.MeetingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DefaultLivekitRoomServiceTest {

    private final DefaultLivekitRoomService sut = new DefaultLivekitRoomService();

    @Test
    void createRoom_deberiaCrearRoomConValoresEsperados() {
        // given
        String roomName = "room-1";
        LivekitRoom.RoomOptions options = LivekitRoom.RoomOptions.builder().build();

        // when
        LivekitRoom room = sut.createRoom(roomName, options);

        // then
        assertNotNull(room);
        assertEquals(roomName, room.getName());
        assertFalse(room.isActive());
        assertEquals(0, room.getParticipantCount());
        assertNotNull(room.getCreatedAt());
        assertTrue(room.getCreatedAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        assertEquals(options, room.getOptions());
    }

    @Test
    void getRoom_deberiaRetornarEmpty() {
        // when
        Optional<LivekitRoom> result = sut.getRoom("room-1");

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void deleteRoom_deberiaRetornarTrue() {
        // when
        boolean result = sut.deleteRoom("room-1");

        // then
        assertTrue(result);
    }

    @Test
    void getParticipants_deberiaRetornarListaVacia() {
        // when
        List<String> result = sut.getParticipants("room-1");

        // then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRoomStatus_deberiaRetornarStatusBasico() {
        // given
        String roomName = "room-1";

        // when
        MeetingStatus status = sut.getRoomStatus(roomName);

        // then
        assertNotNull(status);
        assertTrue(status.isExists());
        assertFalse(status.isActive());
        assertEquals(0, status.getParticipantCount());
        assertEquals(roomName, status.getRoomName());
        assertNotNull(status.getStartedAt());
        assertTrue(status.getCreationTime() > 0);
    }
}
