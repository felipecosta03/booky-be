package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.model.*;
import com.uade.bookybe.core.model.constant.GamificationActivity;
import com.uade.bookybe.core.usecase.GamificationService;
import com.uade.bookybe.infraestructure.entity.*;
import com.uade.bookybe.infraestructure.mapper.*;
import com.uade.bookybe.infraestructure.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class GamificationServiceImpl implements GamificationService {

  private final GamificationProfileRepository gamificationProfileRepository;
  private final UserAchievementRepository userAchievementRepository;
  private final AchievementRepository achievementRepository;
  private final UserLevelRepository userLevelRepository;

  // Note: Points are now configured in GamificationActivity enum

  @Override
  public Optional<GamificationProfile> initializeUserProfile(String userId) {
    log.info("Initializing gamification profile for user: {}", userId);

    // Check if profile already exists
    if (gamificationProfileRepository.findByUserId(userId).isPresent()) {
      log.warn("Gamification profile already exists for user: {}", userId);
      return getUserProfile(userId);
    }

    GamificationProfileEntity entity =
        GamificationProfileEntity.builder()
            .id("gp-" + UUID.randomUUID().toString().substring(0, 8))
            .userId(userId)
            .totalPoints(0)
            .currentLevel(1)
            .dateCreated(LocalDateTime.now())
            .lastActivity(LocalDateTime.now())
            .build();

    GamificationProfileEntity savedEntity = gamificationProfileRepository.save(entity);
    GamificationProfile profile = GamificationProfileEntityMapper.INSTANCE.toModel(savedEntity);

    // Enrich profile with additional data
    enrichProfile(profile);

    return Optional.of(profile);
  }

  @Override
  public Optional<GamificationProfile> getUserProfile(String userId) {
    Optional<GamificationProfileEntity> profileEntity =
        gamificationProfileRepository.findByUserId(userId);

    // Auto-inicializar si no existe
    if (profileEntity.isEmpty()) {
      log.info("Auto-initializing gamification profile for user: {}", userId);
      return initializeUserProfile(userId);
    }

    return profileEntity.map(
        entity -> {
          GamificationProfile profile = GamificationProfileEntityMapper.INSTANCE.toModel(entity);
          enrichProfile(profile);
          return profile;
        });
  }



  @Override
  public Optional<GamificationProfile> awardPoints(String userId, String action, int points) {
    log.info("Awarding {} points to user {} for action: {}", points, userId, action);

    Optional<GamificationProfileEntity> optionalEntity =
        gamificationProfileRepository.findByUserId(userId);
    if (optionalEntity.isEmpty()) {
      log.info("Auto-initializing gamification profile for user: {}", userId);
      return initializeUserProfile(userId).flatMap(profile -> awardPoints(userId, action, points));
    }

    GamificationProfileEntity entity = optionalEntity.get();
    int oldPoints = entity.getTotalPoints();
    int newPoints = oldPoints + points;

    entity.setTotalPoints(newPoints);
    entity.setLastActivity(LocalDateTime.now());

    // Update level if necessary
    updateUserLevel(entity, newPoints);

    GamificationProfileEntity savedEntity = gamificationProfileRepository.save(entity);
    GamificationProfile profile = GamificationProfileEntityMapper.INSTANCE.toModel(savedEntity);

    // Check for new achievements
    checkAndAwardAchievements(userId);

    // Enrich profile with additional data
    enrichProfile(profile);

    return Optional.of(profile);
  }

  /** Award points for specific activity using enum */
  public Optional<GamificationProfile> awardPoints(String userId, GamificationActivity activity) {
    log.info(
        "Awarding {} points to user {} for activity: {} ({})",
        activity.getPoints(),
        userId,
        activity.name(),
        activity.getDescription());

    return awardPoints(userId, activity.name(), activity.getPoints());
  }

  @Override
  public Optional<GamificationProfile> processBookAdded(String userId) {
    updateActivityCounter(userId, "booksRead", 0); // Just added, not read yet
    return awardPoints(userId, GamificationActivity.BOOK_ADDED);
  }

  @Override
  public Optional<GamificationProfile> processBookRead(String userId) {
    updateActivityCounter(userId, "booksRead", 1);
    return awardPoints(userId, GamificationActivity.BOOK_READ);
  }

  @Override
  public Optional<GamificationProfile> processBookFavorited(String userId) {
    return awardPoints(userId, GamificationActivity.BOOK_FAVORITED);
  }

  @Override
  public Optional<GamificationProfile> processBookOfferedForExchange(String userId) {
    return awardPoints(userId, GamificationActivity.BOOK_OFFERED_FOR_EXCHANGE);
  }

  @Override
  public Optional<GamificationProfile> processExchangeCreated(String userId) {
    return awardPoints(userId, GamificationActivity.EXCHANGE_CREATED);
  }

  @Override
  public Optional<GamificationProfile> processExchangeCompleted(String userId) {
    updateActivityCounter(userId, "exchangesCompleted", 1);
    return awardPoints(userId, GamificationActivity.EXCHANGE_COMPLETED);
  }

  @Override
  public Optional<GamificationProfile> processPostCreated(String userId) {
    updateActivityCounter(userId, "postsCreated", 1);
    return awardPoints(userId, GamificationActivity.POST_CREATED);
  }

  @Override
  public Optional<GamificationProfile> processCommentCreated(String userId) {
    updateActivityCounter(userId, "commentsCreated", 1);
    return awardPoints(userId, GamificationActivity.COMMENT_CREATED);
  }

  @Override
  public Optional<GamificationProfile> processCommunityJoined(String userId) {
    updateActivityCounter(userId, "communitiesJoined", 1);
    return awardPoints(userId, GamificationActivity.COMMUNITY_JOINED);
  }

  @Override
  public Optional<GamificationProfile> processCommunityCreated(String userId) {
    updateActivityCounter(userId, "communitiesCreated", 1);
    return awardPoints(userId, GamificationActivity.COMMUNITY_CREATED);
  }

  @Override
  public Optional<GamificationProfile> processReadingClubJoined(String userId) {
    updateActivityCounter(userId, "readingClubsJoined", 1);
    return awardPoints(userId, GamificationActivity.READING_CLUB_JOINED);
  }

  @Override
  public Optional<GamificationProfile> processReadingClubCreated(String userId) {
    updateActivityCounter(userId, "readingClubsCreated", 1);
    return awardPoints(userId, GamificationActivity.READING_CLUB_CREATED);
  }

  @Override
  public List<Achievement> getAllAchievements() {
    return achievementRepository.findByIsActiveTrue().stream()
        .map(AchievementEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public List<UserAchievement> getUserAchievements(String userId) {
    return userAchievementRepository.findByUserIdOrderByDateEarnedDesc(userId).stream()
        .map(UserAchievementEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public List<UserAchievement> getUnnotifiedAchievements(String userId) {
    return userAchievementRepository.findUnnotifiedByUserId(userId).stream()
        .map(UserAchievementEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public void markAchievementsAsNotified(String userId, List<String> achievementIds) {
    for (String achievementId : achievementIds) {
      userAchievementRepository
          .findByUserIdAndAchievementId(userId, achievementId)
          .ifPresent(
              entity -> {
                entity.setNotified(true);
                userAchievementRepository.save(entity);
              });
    }
  }

  @Override
  public List<UserLevel> getAllUserLevels() {
    return userLevelRepository.findAll().stream()
        .map(UserLevelEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<UserLevel> getLevelForPoints(int points) {
    return userLevelRepository
        .findHighestLevelForPoints(points)
        .map(UserLevelEntityMapper.INSTANCE::toModel);
  }

  @Override
  public List<UserAchievement> checkAndAwardAchievements(String userId) {
    log.info("Checking achievements for user: {}", userId);

    Optional<GamificationProfileEntity> optionalProfile =
        gamificationProfileRepository.findByUserId(userId);
    if (optionalProfile.isEmpty()) {
      return Collections.emptyList();
    }

    GamificationProfileEntity profile = optionalProfile.get();
    List<AchievementEntity> allAchievements = achievementRepository.findByIsActiveTrue();
    List<UserAchievement> newAchievements = new ArrayList<>();

    for (AchievementEntity achievement : allAchievements) {
      // Check if user already has this achievement
      if (userAchievementRepository.existsByUserIdAndAchievementId(userId, achievement.getId())) {
        continue;
      }

      // Check if user meets the achievement requirements
      if (meetsAchievementRequirements(profile, achievement)) {
        UserAchievementEntity userAchievement =
            UserAchievementEntity.builder()
                .id("ua-" + UUID.randomUUID().toString().substring(0, 8))
                .userId(userId)
                .achievementId(achievement.getId())
                .dateEarned(LocalDateTime.now())
                .notified(false)
                .build();

        UserAchievementEntity savedAchievement = userAchievementRepository.save(userAchievement);

        // Award additional points for the achievement
        profile.setTotalPoints(profile.getTotalPoints() + achievement.getPointsReward());
        gamificationProfileRepository.save(profile);

        UserAchievement userAchievementModel =
            UserAchievementEntityMapper.INSTANCE.toModel(savedAchievement);
        userAchievementModel.setAchievement(AchievementEntityMapper.INSTANCE.toModel(achievement));
        newAchievements.add(userAchievementModel);

        log.info("Achievement {} awarded to user {}", achievement.getName(), userId);
      }
    }

    return newAchievements;
  }

  // Private helper methods

  private void enrichProfile(GamificationProfile profile) {
    // Add current level information
    getLevelForPoints(profile.getTotalPoints()).ifPresent(profile::setUserLevel);

    // Add achievements
    profile.setAchievements(getUserAchievements(profile.getUserId()));

    // Calculate points to next level
    profile.setPointsToNextLevel(calculatePointsToNextLevel(profile.getTotalPoints()));
  }

  private int calculatePointsToNextLevel(int currentPoints) {
    return userLevelRepository.findAll().stream()
        .filter(level -> level.getMinPoints() > currentPoints)
        .min(Comparator.comparingInt(UserLevelEntity::getMinPoints))
        .map(nextLevel -> nextLevel.getMinPoints() - currentPoints)
        .orElse(0);
  }

  private void updateUserLevel(GamificationProfileEntity entity, int newPoints) {
    userLevelRepository
        .findHighestLevelForPoints(newPoints)
        .ifPresent(level -> entity.setCurrentLevel(level.getLevel()));
  }

  private void updateActivityCounter(String userId, String counterType, int increment) {
    gamificationProfileRepository
        .findByUserId(userId)
        .ifPresent(
            entity -> {
              switch (counterType) {
                case "booksRead" -> entity.setBooksRead(entity.getBooksRead() + increment);
                case "exchangesCompleted" ->
                    entity.setExchangesCompleted(entity.getExchangesCompleted() + increment);
                case "postsCreated" -> entity.setPostsCreated(entity.getPostsCreated() + increment);
                case "commentsCreated" ->
                    entity.setCommentsCreated(entity.getCommentsCreated() + increment);
                case "communitiesJoined" ->
                    entity.setCommunitiesJoined(entity.getCommunitiesJoined() + increment);
                case "communitiesCreated" ->
                    entity.setCommunitiesCreated(entity.getCommunitiesCreated() + increment);
                case "readingClubsJoined" ->
                    entity.setReadingClubsJoined(entity.getReadingClubsJoined() + increment);
                case "readingClubsCreated" ->
                    entity.setReadingClubsCreated(entity.getReadingClubsCreated() + increment);
              }
              entity.setLastActivity(LocalDateTime.now());
              gamificationProfileRepository.save(entity);
            });
  }

  private boolean meetsAchievementRequirements(
      GamificationProfileEntity profile, AchievementEntity achievement) {
    return switch (achievement.getCondition()) {
      case "BOOKS_READ" -> profile.getBooksRead() >= achievement.getRequiredValue();
      case "EXCHANGES_COMPLETED" ->
          profile.getExchangesCompleted() >= achievement.getRequiredValue();
      case "POSTS_CREATED" -> profile.getPostsCreated() >= achievement.getRequiredValue();
      case "COMMENTS_CREATED" -> profile.getCommentsCreated() >= achievement.getRequiredValue();
      case "COMMUNITIES_JOINED" -> profile.getCommunitiesJoined() >= achievement.getRequiredValue();
      case "COMMUNITIES_CREATED" ->
          profile.getCommunitiesCreated() >= achievement.getRequiredValue();
      case "READING_CLUBS_JOINED" ->
          profile.getReadingClubsJoined() >= achievement.getRequiredValue();
      case "READING_CLUBS_CREATED" ->
          profile.getReadingClubsCreated() >= achievement.getRequiredValue();
      case "TOTAL_POINTS" -> profile.getTotalPoints() >= achievement.getRequiredValue();
      default -> false;
    };
  }

  @Override
  public boolean deleteUserGamificationData(String userId) {
    log.info("Manually deleting gamification data for user: {}", userId);
    
    try {
      // Delete user achievements first
      List<UserAchievementEntity> userAchievements = userAchievementRepository.findByUserId(userId);
      if (!userAchievements.isEmpty()) {
        userAchievementRepository.deleteAll(userAchievements);
        log.info("Deleted {} user achievements for user: {}", userAchievements.size(), userId);
      }
      
      // Delete gamification profile
      Optional<GamificationProfileEntity> profile = gamificationProfileRepository.findByUserId(userId);
      if (profile.isPresent()) {
        gamificationProfileRepository.delete(profile.get());
        log.info("Deleted gamification profile for user: {}", userId);
      }
      
      log.info("Successfully deleted all gamification data for user: {}", userId);
      return true;
      
    } catch (Exception e) {
      log.error("Error deleting gamification data for user {}: {}", userId, e.getMessage(), e);
      return false;
    }
  }
}
