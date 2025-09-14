package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.LivekitToken;
import com.uade.bookybe.core.model.MeetingStatus;
import com.uade.bookybe.core.model.ReadingClub;

import java.util.Optional;

public interface MeetingService {
    LivekitToken generateMeetingToken(String readingClubId, String userId, String participantName);
    ReadingClub startMeeting(String readingClubId, String moderatorId);
    ReadingClub endMeeting(String readingClubId, String moderatorId);
    MeetingStatus getMeetingStatus(String readingClubId);
    boolean isMemberOfReadingClub(String userId, String readingClubId);
    Optional<ReadingClub> getReadingClub(String readingClubId);
}
