# LiveKit Service para Spring Boot

## Dependencias Maven

Agrega estas dependencias a tu `pom.xml`:

```xml
<dependencies>
    <!-- LiveKit Server SDK -->
    <dependency>
        <groupId>io.livekit</groupId>
        <artifactId>livekit-server</artifactId>
        <version>0.6.1</version>
    </dependency>
    
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
</dependencies>
```

## Configuración

### application.yml

```yaml
# application.yml
livekit:
  api-key: APIQTZk4A9komWw
  api-secret: L1TojB8qfGbD26QIawCSop0tRAxQrtTs627URLD2KUO
  ws-url: wss://booky-rru3jofi.livekit.cloud

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/booky
    username: ${DB_USERNAME:booky}
    password: ${DB_PASSWORD:password}
  
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

### Configuration Class

```java
// config/LiveKitConfig.java
package com.booky.config;

import io.livekit.server.RoomServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {
    
    @Value("${livekit.api-key}")
    private String apiKey;
    
    @Value("${livekit.api-secret}")
    private String apiSecret;
    
    @Value("${livekit.ws-url}")
    private String wsUrl;
    
    @Bean
    public RoomServiceClient roomServiceClient() {
        return RoomServiceClient.create(wsUrl, apiKey, apiSecret);
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
}
```

## Service Implementation

### LiveKit Service

```java
// service/LiveKitService.java
package com.booky.service;

import com.booky.config.LiveKitConfig;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomServiceClient;
import livekit.LivekitModels.Room;
import livekit.LivekitModels.Participant;
import livekit.LivekitRoom.CreateRoomRequest;
import livekit.LivekitRoom.DeleteRoomRequest;
import livekit.LivekitRoom.ListRoomsRequest;
import livekit.LivekitRoom.ListParticipantsRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LiveKitService {
    
    private static final Logger logger = LoggerFactory.getLogger(LiveKitService.class);
    
    @Autowired
    private RoomServiceClient roomServiceClient;
    
    @Autowired
    private LiveKitConfig liveKitConfig;
    
    /**
     * Create a LiveKit access token for a participant
     */
    public String createToken(String roomName, String participantName, String participantId, TokenPermissions permissions) {
        try {
            AccessToken token = new AccessToken(
                liveKitConfig.getApiKey(),
                liveKitConfig.getApiSecret()
            );
            
            // Set participant identity and name
            token.setIdentity(participantId);
            token.setName(participantName);
            
            // Set token expiration (6 hours)
            token.setTtl(Duration.ofHours(6));
            
            // Add room grant with permissions
            AccessToken.VideoGrant grant = new AccessToken.VideoGrant();
            grant.setRoom(roomName);
            grant.setRoomJoin(true);
            grant.setCanPublish(permissions.canPublish);
            grant.setCanSubscribe(permissions.canSubscribe);
            grant.setCanPublishData(permissions.canPublishData);
            grant.setRoomAdmin(permissions.isModerator);
            grant.setRoomRecord(permissions.canRecord);
            
            token.addGrant(grant);
            
            return token.toJwt();
            
        } catch (Exception e) {
            logger.error("Error creating LiveKit token", e);
            throw new RuntimeException("Failed to create access token", e);
        }
    }
    
    /**
     * Create or get a room
     */
    public Room createRoom(String roomName, RoomOptions options) {
        try {
            CreateRoomRequest.Builder requestBuilder = CreateRoomRequest.newBuilder()
                .setName(roomName)
                .setEmptyTimeout(options.emptyTimeout)
                .setMaxParticipants(options.maxParticipants);
            
            if (options.metadata != null) {
                requestBuilder.setMetadata(options.metadata);
            }
            
            return roomServiceClient.createRoom(requestBuilder.build());
            
        } catch (Exception e) {
            // Room might already exist
            if (e.getMessage().contains("already exists")) {
                return getRoom(roomName).orElse(null);
            }
            logger.error("Error creating room: " + roomName, e);
            throw new RuntimeException("Failed to create room", e);
        }
    }
    
    /**
     * Get room information
     */
    public Optional<Room> getRoom(String roomName) {
        try {
            ListRoomsRequest request = ListRoomsRequest.newBuilder()
                .addNames(roomName)
                .build();
            
            List<Room> rooms = roomServiceClient.listRooms(request);
            return rooms.isEmpty() ? Optional.empty() : Optional.of(rooms.get(0));
            
        } catch (Exception e) {
            logger.error("Error getting room: " + roomName, e);
            return Optional.empty();
        }
    }
    
    /**
     * Delete a room (ends meeting for all participants)
     */
    public boolean deleteRoom(String roomName) {
        try {
            DeleteRoomRequest request = DeleteRoomRequest.newBuilder()
                .setRoom(roomName)
                .build();
            
            roomServiceClient.deleteRoom(request);
            return true;
            
        } catch (Exception e) {
            logger.error("Error deleting room: " + roomName, e);
            return false;
        }
    }
    
    /**
     * Get list of participants in a room
     */
    public List<Participant> getParticipants(String roomName) {
        try {
            ListParticipantsRequest request = ListParticipantsRequest.newBuilder()
                .setRoom(roomName)
                .build();
            
            return roomServiceClient.listParticipants(request);
            
        } catch (Exception e) {
            logger.error("Error getting participants for room: " + roomName, e);
            return List.of();
        }
    }
    
    /**
     * Check if a room is active (has participants)
     */
    public RoomStatus getRoomStatus(String roomName) {
        try {
            Optional<Room> roomOpt = getRoom(roomName);
            
            if (roomOpt.isEmpty()) {
                return new RoomStatus(false, false, 0, null);
            }
            
            Room room = roomOpt.get();
            boolean isActive = room.getNumParticipants() > 0;
            
            return new RoomStatus(
                true,
                isActive,
                room.getNumParticipants(),
                room.getCreationTime()
            );
            
        } catch (Exception e) {
            logger.error("Error getting room status: " + roomName, e);
            return new RoomStatus(false, false, 0, null);
        }
    }
    
    // Helper classes
    public static class TokenPermissions {
        public boolean canPublish = true;
        public boolean canSubscribe = true;
        public boolean canPublishData = true;
        public boolean isModerator = false;
        public boolean canRecord = false;
        
        public TokenPermissions() {}
        
        public TokenPermissions(boolean isModerator) {
            this.isModerator = isModerator;
            this.canRecord = isModerator;
        }
    }
    
    public static class RoomOptions {
        public int emptyTimeout = 300; // 5 minutes
        public int maxParticipants = 50;
        public String metadata;
        
        public RoomOptions() {}
        
        public RoomOptions(int emptyTimeout, int maxParticipants, String metadata) {
            this.emptyTimeout = emptyTimeout;
            this.maxParticipants = maxParticipants;
            this.metadata = metadata;
        }
    }
    
    public static class RoomStatus {
        public boolean exists;
        public boolean isActive;
        public int participantCount;
        public long creationTime;
        
        public RoomStatus(boolean exists, boolean isActive, int participantCount, Long creationTime) {
            this.exists = exists;
            this.isActive = isActive;
            this.participantCount = participantCount;
            this.creationTime = creationTime != null ? creationTime : 0;
        }
    }
}
```

## Controller Implementation

### Meeting Controller

```java
// controller/MeetingController.java
package com.booky.controller;

import com.booky.dto.TokenRequest;
import com.booky.dto.TokenResponse;
import com.booky.dto.MeetingStatusResponse;
import com.booky.entity.ReadingClub;
import com.booky.entity.User;
import com.booky.service.LiveKitService;
import com.booky.service.ReadingClubService;
import com.booky.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reading-clubs/meeting")
public class MeetingController {
    
    private static final Logger logger = LoggerFactory.getLogger(MeetingController.class);
    
    @Autowired
    private LiveKitService liveKitService;
    
    @Autowired
    private ReadingClubService readingClubService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Generate token for joining a meeting
     */
    @PostMapping("/token")
    public ResponseEntity<?> generateToken(@RequestBody TokenRequest request, Authentication auth) {
        try {
            String userId = auth.getName();
            User user = userService.findById(userId);
            
            // Verify user is member of the reading club
            ReadingClub club = readingClubService.findById(request.getReadingClubId());
            if (club == null) {
                return ResponseEntity.notFound().build();
            }
            
            boolean isMember = readingClubService.isMember(userId, request.getReadingClubId());
            if (!isMember) {
                return ResponseEntity.status(403).body(Map.of("error", "Not a member of this reading club"));
            }
            
            // Check if user is moderator
            boolean isModerator = club.getModeratorId().equals(userId);
            
            // Generate room name
            String roomName = "reading-club-" + request.getReadingClubId();
            
            // Create token with appropriate permissions
            LiveKitService.TokenPermissions permissions = new LiveKitService.TokenPermissions(isModerator);
            
            String token = liveKitService.createToken(
                roomName,
                request.getParticipantName(),
                userId,
                permissions
            );
            
            TokenResponse response = new TokenResponse();
            response.setToken(token);
            response.setRoomName(roomName);
            response.setParticipantName(request.getParticipantName());
            response.setModerator(isModerator);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error generating token", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to generate token"));
        }
    }
    
    /**
     * Start a meeting (moderator only)
     */
    @PostMapping("/{clubId}/start")
    public ResponseEntity<?> startMeeting(@PathVariable String clubId, Authentication auth) {
        try {
            String userId = auth.getName();
            
            // Verify user is moderator
            ReadingClub club = readingClubService.findById(clubId);
            if (club == null || !club.getModeratorId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only moderators can start meetings"));
            }
            
            String roomName = "reading-club-" + clubId;
            
            // Create room with metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("clubId", clubId);
            metadata.put("clubName", club.getName());
            metadata.put("moderatorId", userId);
            metadata.put("startedAt", LocalDateTime.now().toString());
            
            LiveKitService.RoomOptions options = new LiveKitService.RoomOptions(
                600, // 10 minutes empty timeout
                50,  // max participants
                objectMapper.writeValueAsString(metadata)
            );
            
            liveKitService.createRoom(roomName, options);
            
            // Update database
            club.setMeetingActive(true);
            club.setMeetingStartedAt(LocalDateTime.now());
            readingClubService.save(club);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("roomName", roomName);
            response.put("startedAt", LocalDateTime.now().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error starting meeting", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to start meeting"));
        }
    }
    
    /**
     * End meeting (moderator only)
     */
    @PostMapping("/{clubId}/end")
    public ResponseEntity<?> endMeeting(@PathVariable String clubId, Authentication auth) {
        try {
            String userId = auth.getName();
            
            // Verify user is moderator
            ReadingClub club = readingClubService.findById(clubId);
            if (club == null || !club.getModeratorId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only moderators can end meetings"));
            }
            
            String roomName = "reading-club-" + clubId;
            
            // Delete room (disconnects all participants)
            boolean deleted = liveKitService.deleteRoom(roomName);
            
            // Update database
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = club.getMeetingStartedAt();
            long duration = startTime != null ? 
                java.time.Duration.between(startTime, endTime).getSeconds() : 0;
            
            club.setMeetingActive(false);
            club.setMeetingEndedAt(endTime);
            club.setLastMeetingDuration(duration);
            readingClubService.save(club);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", deleted);
            response.put("endedAt", endTime.toString());
            response.put("duration", duration);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error ending meeting", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to end meeting"));
        }
    }
    
    /**
     * Get meeting status
     */
    @GetMapping("/{clubId}/status")
    public ResponseEntity<?> getMeetingStatus(@PathVariable String clubId) {
        try {
            String roomName = "reading-club-" + clubId;
            
            // Get room status from LiveKit
            LiveKitService.RoomStatus status = liveKitService.getRoomStatus(roomName);
            
            // Get database info
            ReadingClub club = readingClubService.findById(clubId);
            
            MeetingStatusResponse response = new MeetingStatusResponse();
            response.setActive(status.isActive);
            response.setParticipantCount(status.participantCount);
            response.setStartedAt(club != null && club.getMeetingStartedAt() != null ? 
                club.getMeetingStartedAt().toString() : null);
            response.setRoomName(status.exists ? roomName : null);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error getting meeting status", e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to get meeting status"));
        }
    }
}
```

## DTOs

### Request/Response DTOs

```java
// dto/TokenRequest.java
package com.booky.dto;

public class TokenRequest {
    private String readingClubId;
    private String participantName;
    
    // Getters and setters
    public String getReadingClubId() { return readingClubId; }
    public void setReadingClubId(String readingClubId) { this.readingClubId = readingClubId; }
    
    public String getParticipantName() { return participantName; }
    public void setParticipantName(String participantName) { this.participantName = participantName; }
}

// dto/TokenResponse.java
package com.booky.dto;

public class TokenResponse {
    private String token;
    private String roomName;
    private String participantName;
    private boolean isModerator;
    
    // Getters and setters
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    
    public String getParticipantName() { return participantName; }
    public void setParticipantName(String participantName) { this.participantName = participantName; }
    
    public boolean isModerator() { return isModerator; }
    public void setModerator(boolean moderator) { isModerator = moderator; }
}

// dto/MeetingStatusResponse.java
package com.booky.dto;

public class MeetingStatusResponse {
    private boolean isActive;
    private int participantCount;
    private String startedAt;
    private String roomName;
    
    // Getters and setters
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public int getParticipantCount() { return participantCount; }
    public void setParticipantCount(int participantCount) { this.participantCount = participantCount; }
    
    public String getStartedAt() { return startedAt; }
    public void setStartedAt(String startedAt) { this.startedAt = startedAt; }
    
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
}
```

## Entity Updates

### ReadingClub Entity

```java
// entity/ReadingClub.java
package com.booky.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reading_clubs")
public class ReadingClub {
    
    @Id
    private String id;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "moderator_id")
    private String moderatorId;
    
    // LiveKit meeting fields
    @Column(name = "meeting_active")
    private Boolean meetingActive = false;
    
    @Column(name = "meeting_started_at")
    private LocalDateTime meetingStartedAt;
    
    @Column(name = "meeting_ended_at")
    private LocalDateTime meetingEndedAt;
    
    @Column(name = "last_meeting_duration")
    private Long lastMeetingDuration = 0L;
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getModeratorId() { return moderatorId; }
    public void setModeratorId(String moderatorId) { this.moderatorId = moderatorId; }
    
    public Boolean getMeetingActive() { return meetingActive; }
    public void setMeetingActive(Boolean meetingActive) { this.meetingActive = meetingActive; }
    
    public LocalDateTime getMeetingStartedAt() { return meetingStartedAt; }
    public void setMeetingStartedAt(LocalDateTime meetingStartedAt) { this.meetingStartedAt = meetingStartedAt; }
    
    public LocalDateTime getMeetingEndedAt() { return meetingEndedAt; }
    public void setMeetingEndedAt(LocalDateTime meetingEndedAt) { this.meetingEndedAt = meetingEndedAt; }
    
    public Long getLastMeetingDuration() { return lastMeetingDuration; }
    public void setLastMeetingDuration(Long lastMeetingDuration) { this.lastMeetingDuration = lastMeetingDuration; }
}
```

## Testing

### Test Controller

```java
// test/MeetingControllerTest.java
package com.booky.controller;

import com.booky.service.LiveKitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MeetingController.class)
public class MeetingControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private LiveKitService liveKitService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @WithMockUser(username = "user123")
    public void testGenerateToken() throws Exception {
        // Test implementation
        mockMvc.perform(post("/api/reading-clubs/meeting/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"readingClubId\":\"club123\",\"participantName\":\"Test User\"}"))
                .andExpect(status().isOk());
    }
}
```

## Características Principales

1. **✅ Configuración con Spring Boot**
2. **✅ Inyección de dependencias**
3. **✅ Manejo de errores robusto**
4. **✅ DTOs para requests/responses**
5. **✅ Integración con JPA/Hibernate**
6. **✅ Seguridad con Spring Security**
7. **✅ Logging con SLF4J**
8. **✅ Tests unitarios**

¿Te gustaría que ajuste algo específico de la implementación de Spring Boot o necesitas ayuda con alguna parte en particular?
