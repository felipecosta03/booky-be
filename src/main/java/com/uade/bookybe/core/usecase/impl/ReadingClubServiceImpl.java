package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.ConflictException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.ReadingClubService;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.ReadingClubEntity;
import com.uade.bookybe.infraestructure.entity.ReadingClubMemberEntity;
import com.uade.bookybe.infraestructure.mapper.ReadingClubEntityMapper;
import com.uade.bookybe.infraestructure.repository.BookRepository;
import com.uade.bookybe.infraestructure.repository.CommunityRepository;
import com.uade.bookybe.infraestructure.repository.ReadingClubMemberRepository;
import com.uade.bookybe.infraestructure.repository.ReadingClubRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingClubServiceImpl implements ReadingClubService {

  private final ReadingClubRepository readingClubRepository;
  private final ReadingClubMemberRepository readingClubMemberRepository;
  private final CommunityRepository communityRepository;
  private final BookRepository bookRepository;
  private final GamificationService gamificationService;

  @Override
  public List<ReadingClub> getAllReadingClubs() {
    log.info("Getting all reading clubs");
    List<ReadingClubEntity> entities = readingClubRepository.findAll();
    List<ReadingClub> clubs =
        entities.stream()
            .map(ReadingClubEntityMapper.INSTANCE::toModel)
            .collect(Collectors.toList());

    // Set member count for each club
    clubs.forEach(club -> club.setMemberCount(getMemberCount(club.getId())));

    log.info("Found {} reading clubs", clubs.size());
    return clubs;
  }

  @Override
  public Optional<ReadingClub> getReadingClubById(String id) {
    log.info("Getting reading club by ID: {}", id);
    Optional<ReadingClubEntity> entity = readingClubRepository.findById(id);

    if (entity.isPresent()) {
      ReadingClub club = ReadingClubEntityMapper.INSTANCE.toModel(entity.get());
      club.setMemberCount(getMemberCount(id));
      return Optional.of(club);
    }

    return Optional.empty();
  }

  @Override
  public List<ReadingClub> getReadingClubsByUserId(String userId) {
    log.info("Getting reading clubs for user: {}", userId);
    List<ReadingClubMemberEntity> memberEntities =
        readingClubMemberRepository.findByUserIdWithReadingClub(userId);

    List<ReadingClub> clubs =
        memberEntities.stream()
            .map(
                memberEntity ->
                    ReadingClubEntityMapper.INSTANCE.toModel(memberEntity.getReadingClub()))
            .collect(Collectors.toList());

    // Set member count for each club
    clubs.forEach(club -> club.setMemberCount(getMemberCount(club.getId())));

    log.info("Found {} reading clubs for user: {}", clubs.size(), userId);
    return clubs;
  }

  @Override
  public List<ReadingClub> getReadingClubsByCommunityId(String communityId) {
    log.info("Getting reading clubs for community: {}", communityId);
    List<ReadingClubEntity> entities =
        readingClubRepository.findByCommunityIdOrderByDateCreatedDesc(communityId);

    List<ReadingClub> clubs =
        entities.stream()
            .map(ReadingClubEntityMapper.INSTANCE::toModel)
            .collect(Collectors.toList());

    // Set member count for each club
    clubs.forEach(club -> club.setMemberCount(getMemberCount(club.getId())));

    log.info("Found {} reading clubs for community: {}", clubs.size(), communityId);
    return clubs;
  }

  @Override
  @Transactional
  public Optional<ReadingClub> createReadingClub(
      String moderatorId, String name, String description, String communityId, String bookId, LocalDateTime nextMeeting) {
    log.info(
        "Creating reading club: {} by moderator: {} in community: {}",
        name,
        moderatorId,
        communityId);

    // Check if a club with the same name already exists
    if (readingClubRepository.existsByName(name)) {
      log.warn("Reading club with name {} already exists", name);
      return Optional.empty();
    }

    try {
      // Validate that community exists
      if (!communityRepository.existsById(communityId)) {
        log.warn("Community with ID {} does not exist", communityId);
        return Optional.empty();
      }

      // Validate that book exists
      if (!bookRepository.existsById(bookId)) {
        log.warn("Book with ID {} does not exist", bookId);
        return Optional.empty();
      }

      // Create the reading club
      ReadingClubEntity clubEntity =
          ReadingClubEntity.builder()
              .id("club-" + UUID.randomUUID().toString().substring(0, 8))
              .name(name)
              .description(description)
              .communityId(communityId) // OBLIGATORIO
              .bookId(bookId) // OBLIGATORIO
              .moderatorId(moderatorId)
              .nextMeeting(nextMeeting) // OBLIGATORIO
              .dateCreated(LocalDateTime.now())
              .lastUpdated(LocalDateTime.now())
              .build();

      ReadingClubEntity savedClub = readingClubRepository.save(clubEntity);

      // Add moderator as member
      ReadingClubMemberEntity memberEntity =
          ReadingClubMemberEntity.builder()
              .readingClubId(savedClub.getId())
              .userId(moderatorId)
              .build();

      readingClubMemberRepository.save(memberEntity);

      ReadingClub club = ReadingClubEntityMapper.INSTANCE.toModel(savedClub);
      club.setMemberCount(1L); // Just the moderator for now

      log.info("Reading club created successfully: {}", savedClub.getId());
      
      // Award gamification points for creating reading club
      gamificationService.processReadingClubCreated(moderatorId);
      
      return Optional.of(club);

    } catch (Exception e) {
      log.error("Error creating reading club: {}", e.getMessage(), e);
      return Optional.empty();
    }
  }

  @Override
  @Transactional
  public boolean joinReadingClub(String clubId, String userId) {
    log.info("User {} joining reading club: {}", userId, clubId);
    // Check if user is already a member
    if (readingClubMemberRepository.existsByReadingClubIdAndUserId(clubId, userId)) {
      throw new ConflictException("User is already a member of reading club");
    }

    // Check if reading club exists
    if (!readingClubRepository.existsById(clubId)) {
      throw new NotFoundException("Reading club not found with id: " + clubId);
    }

    // Add user as member
    ReadingClubMemberEntity memberEntity =
        ReadingClubMemberEntity.builder().readingClubId(clubId).userId(userId).build();

    readingClubMemberRepository.save(memberEntity);

    log.info("User {} successfully joined reading club: {}", userId, clubId);
    
    // Award gamification points for joining reading club (only if not moderator joining own club)
    try {
      Optional<ReadingClubEntity> club = readingClubRepository.findById(clubId);
      if (club.isPresent() && !club.get().getModeratorId().equals(userId)) {
        gamificationService.processReadingClubJoined(userId);
      }
    } catch (Exception e) {
      log.warn("Could not award gamification points for joining reading club: {}", e.getMessage());
    }
    
    return true;
  }

  @Override
  public boolean leaveReadingClub(String clubId, String userId) {
    log.info("User {} leaving reading club: {}", userId, clubId);

    // Check if reading club exists (outside transaction)
    if (!readingClubRepository.existsById(clubId)) {
      throw new NotFoundException("Reading club not found with id: " + clubId);
    }

    // Check if user is a member (outside transaction)
    if (!readingClubMemberRepository.existsByReadingClubIdAndUserId(clubId, userId)) {
      log.warn("User {} is not a member of reading club: {}", userId, clubId);
      return false; // Return false instead of throwing exception
    }

    try {
      readingClubMemberRepository.leaveFromReadingClub(clubId, userId);
      log.info("User {} successfully left reading club: {}", userId, clubId);
      return true;
    } catch (Exception e) {
      log.error(
          "Error removing user {} from reading club {}: {}", userId, clubId, e.getMessage(), e);
      return false;
    }
  }

  @Override
  public boolean deleteReadingClub(String clubId, String userId) {
    log.info("User {} attempting to delete reading club: {}", userId, clubId);

    // Check if reading club exists (outside transaction)
    if (!readingClubRepository.existsById(clubId)) {
      throw new NotFoundException("Reading club not found with id: " + clubId);
    }

    // Get the reading club to check if user is the moderator (outside transaction)
    Optional<ReadingClubEntity> clubEntity = readingClubRepository.findById(clubId);
    if (clubEntity.isEmpty()) {
      throw new NotFoundException("Reading club not found with id: " + clubId);
    }

    // Check if user is the moderator (only moderator can delete)
    if (!clubEntity.get().getModeratorId().equals(userId)) {
      log.warn("User {} is not the moderator of reading club: {}", userId, clubId);
      return false; // Not authorized
    }

    // Execute deletion in transaction
    return executeDeleteReadingClub(clubId);
  }

  @Transactional
  private boolean executeDeleteReadingClub(String clubId) {
    try {
      // Delete all members first (cascade deletion)
      readingClubMemberRepository.deleteAllByReadingClubId(clubId);
      log.info("Deleted all members from reading club: {}", clubId);

      // Delete the reading club itself
      readingClubRepository.deleteById(clubId);
      log.info("Reading club deleted successfully: {}", clubId);

      return true;
    } catch (Exception e) {
      log.error("Error deleting reading club {}: {}", clubId, e.getMessage(), e);
      return false;
    }
  }

  @Override
  public List<ReadingClub> searchReadingClubs(String query) {
    log.info("Searching reading clubs with query: {}", query);
    List<ReadingClubEntity> entities = readingClubRepository.searchReadingClubs(query);

    List<ReadingClub> clubs =
        entities.stream()
            .map(ReadingClubEntityMapper.INSTANCE::toModel)
            .collect(Collectors.toList());

    // Set member count for each club
    clubs.forEach(club -> club.setMemberCount(getMemberCount(club.getId())));

    log.info("Found {} reading clubs for query: {}", clubs.size(), query);
    return clubs;
  }

  @Override
  public List<ReadingClub> getReadingClubsByBookId(String bookId) {
    log.info("Getting reading clubs for book: {}", bookId);
    List<ReadingClubEntity> entities =
        readingClubRepository.findByBookIdOrderByDateCreatedDesc(bookId);

    List<ReadingClub> clubs =
        entities.stream()
            .map(ReadingClubEntityMapper.INSTANCE::toModel)
            .collect(Collectors.toList());

    // Set member count for each club
    clubs.forEach(club -> club.setMemberCount(getMemberCount(club.getId())));

    log.info("Found {} reading clubs for book: {}", clubs.size(), bookId);
    return clubs;
  }

  @Override
  public long getMemberCount(String clubId) {
    return readingClubMemberRepository.countByReadingClubId(clubId);
  }

  @Override
  @Transactional
  public Optional<ReadingClub> updateMeeting(String clubId, String userId, LocalDateTime nextMeeting, Integer currentChapter) {
    log.info("Updating meeting for reading club: {} by user: {}", clubId, userId);

    try {
      // Check if reading club exists
      Optional<ReadingClubEntity> clubEntityOpt = readingClubRepository.findById(clubId);
      if (clubEntityOpt.isEmpty()) {
        log.warn("Reading club with ID {} not found", clubId);
        return Optional.empty();
      }

      ReadingClubEntity clubEntity = clubEntityOpt.get();

      // Check if user is the moderator
      if (!clubEntity.getModeratorId().equals(userId)) {
        log.warn("User {} is not the moderator of reading club {}", userId, clubId);
        return Optional.empty();
      }

      // Update meeting information
      clubEntity.setNextMeeting(nextMeeting);
      clubEntity.setCurrentChapter(currentChapter);
      clubEntity.setLastUpdated(LocalDateTime.now());

      ReadingClubEntity savedClub = readingClubRepository.save(clubEntity);
      ReadingClub club = ReadingClubEntityMapper.INSTANCE.toModel(savedClub);
      club.setMemberCount(getMemberCount(clubId));

      log.info("Meeting updated successfully for reading club: {}", clubId);
      return Optional.of(club);

    } catch (Exception e) {
      log.error("Error updating meeting for reading club {}: {}", clubId, e.getMessage(), e);
      return Optional.empty();
    }
  }
}
