package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "reading_clubs")
public class ReadingClubEntity {
  @Id private String id;

  @Column(name = "date_created")
  private LocalDateTime dateCreated;

  @Column(length = 1000)
  private String description;

  @Column(name = "last_updated")
  private LocalDateTime lastUpdated;

  private String name;

  @Column(name = "book_id", nullable = false)
  private String bookId;

  @Column(name = "community_id", nullable = false)
  private String communityId;

  @Column(name = "moderator_id", nullable = false)
  private String moderatorId;

  @Column(name = "next_meeting")
  private LocalDateTime nextMeeting;

  @Builder.Default
  @Column(name = "current_chapter")
  private Integer currentChapter = 0;

  // LiveKit meeting fields
  @Builder.Default
  @Column(name = "meeting_active")
  private Boolean meetingActive = false;

  @Column(name = "meeting_started_at")
  private LocalDateTime meetingStartedAt;

  @Column(name = "meeting_ended_at")
  private LocalDateTime meetingEndedAt;

  @Builder.Default
  @Column(name = "last_meeting_duration")
  private Long lastMeetingDuration = 0L;
}
