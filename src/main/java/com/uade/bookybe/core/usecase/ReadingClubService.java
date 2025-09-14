package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.ReadingClub;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReadingClubService {

  List<ReadingClub> getAllReadingClubs();

  Optional<ReadingClub> getReadingClubById(String id);

  List<ReadingClub> getReadingClubsByUserId(String userId);

  List<ReadingClub> getReadingClubsByCommunityId(String communityId);

  Optional<ReadingClub> createReadingClub(
      String moderatorId,
      String name,
      String description,
      String communityId,
      String bookId,
      LocalDateTime nextMeeting);

  boolean joinReadingClub(String clubId, String userId);

  boolean leaveReadingClub(String clubId, String userId);

  boolean deleteReadingClub(String clubId, String userId);

  List<ReadingClub> searchReadingClubs(String query);

  List<ReadingClub> getReadingClubsByBookId(String bookId);

  long getMemberCount(String clubId);

  Optional<ReadingClub> updateMeeting(
      String clubId, String userId, LocalDateTime nextMeeting, Integer currentChapter);

  boolean isUserMember(String clubId, String userId);
}
