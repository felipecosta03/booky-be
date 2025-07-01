package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.model.ReadingClub;
import com.uade.bookybe.core.usecase.ReadingClubService;
import com.uade.bookybe.infraestructure.entity.ReadingClubEntity;
import com.uade.bookybe.infraestructure.entity.ReadingClubMemberEntity;
import com.uade.bookybe.infraestructure.mapper.ReadingClubEntityMapper;
import com.uade.bookybe.infraestructure.repository.ReadingClubRepository;
import com.uade.bookybe.infraestructure.repository.ReadingClubMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingClubServiceImpl implements ReadingClubService {

  private final ReadingClubRepository readingClubRepository;
  private final ReadingClubMemberRepository readingClubMemberRepository;

  @Override
  public List<ReadingClub> getAllReadingClubs() {
    log.info("Getting all reading clubs");
    List<ReadingClubEntity> entities = readingClubRepository.findAll();
    List<ReadingClub> clubs = entities.stream()
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
    List<ReadingClubMemberEntity> memberEntities = readingClubMemberRepository.findByUserIdWithReadingClub(userId);
    
    List<ReadingClub> clubs = memberEntities.stream()
        .map(memberEntity -> ReadingClubEntityMapper.INSTANCE.toModel(memberEntity.getReadingClub()))
        .collect(Collectors.toList());
    
    // Set member count for each club
    clubs.forEach(club -> club.setMemberCount(getMemberCount(club.getId())));
    
    log.info("Found {} reading clubs for user: {}", clubs.size(), userId);
    return clubs;
  }

  @Override
  public List<ReadingClub> getReadingClubsByCommunityId(String communityId) {
    log.info("Getting reading clubs for community: {}", communityId);
    List<ReadingClubEntity> entities = readingClubRepository.findByCommunityIdOrderByDateCreatedDesc(communityId);
    
    List<ReadingClub> clubs = entities.stream()
        .map(ReadingClubEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
    
    // Set member count for each club
    clubs.forEach(club -> club.setMemberCount(getMemberCount(club.getId())));
    
    log.info("Found {} reading clubs for community: {}", clubs.size(), communityId);
    return clubs;
  }

  @Override
  @Transactional
  public Optional<ReadingClub> createReadingClub(String moderatorId, String name, String description, String bookId) {
    log.info("Creating reading club: {} by moderator: {}", name, moderatorId);

    // Check if a club with the same name already exists
    if (readingClubRepository.existsByName(name)) {
      log.warn("Reading club with name {} already exists", name);
      return Optional.empty();
    }

    try {
      // Create the reading club
      ReadingClubEntity clubEntity = ReadingClubEntity.builder()
          .id("club-" + UUID.randomUUID().toString().substring(0, 8))
          .name(name)
          .description(description)
          .bookId(bookId)
          .moderatorId(moderatorId)
          .dateCreated(LocalDateTime.now())
          .lastUpdated(LocalDateTime.now())
          .build();

      ReadingClubEntity savedClub = readingClubRepository.save(clubEntity);

      // Add moderator as member
      ReadingClubMemberEntity memberEntity = ReadingClubMemberEntity.builder()
          .readingClubId(savedClub.getId())
          .userId(moderatorId)
          .build();

      readingClubMemberRepository.save(memberEntity);

      ReadingClub club = ReadingClubEntityMapper.INSTANCE.toModel(savedClub);
      club.setMemberCount(1L); // Just the moderator for now

      log.info("Reading club created successfully: {}", savedClub.getId());
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

    try {
      // Check if user is already a member
      if (readingClubMemberRepository.existsByReadingClubIdAndUserId(clubId, userId)) {
        log.warn("User {} is already a member of reading club: {}", userId, clubId);
        return false;
      }

      // Check if reading club exists
      if (!readingClubRepository.existsById(clubId)) {
        log.warn("Reading club not found: {}", clubId);
        return false;
      }

      // Add user as member
      ReadingClubMemberEntity memberEntity = ReadingClubMemberEntity.builder()
          .readingClubId(clubId)
          .userId(userId)
          .build();

      readingClubMemberRepository.save(memberEntity);

      log.info("User {} successfully joined reading club: {}", userId, clubId);
      return true;

    } catch (Exception e) {
      log.error("Error joining reading club: {}", e.getMessage(), e);
      return false;
    }
  }

  @Override
  public List<ReadingClub> searchReadingClubs(String query) {
    log.info("Searching reading clubs with query: {}", query);
    List<ReadingClubEntity> entities = readingClubRepository.searchReadingClubs(query);
    
    List<ReadingClub> clubs = entities.stream()
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
    List<ReadingClubEntity> entities = readingClubRepository.findByBookIdOrderByDateCreatedDesc(bookId);
    
    List<ReadingClub> clubs = entities.stream()
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
} 