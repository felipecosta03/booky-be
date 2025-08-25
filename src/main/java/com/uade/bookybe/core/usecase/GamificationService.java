package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.Achievement;
import com.uade.bookybe.core.model.GamificationProfile;
import com.uade.bookybe.core.model.UserAchievement;
import com.uade.bookybe.core.model.UserLevel;
import com.uade.bookybe.core.model.constant.GamificationActivity;

import java.util.List;
import java.util.Optional;

public interface GamificationService {

  /** Initialize gamification profile for a new user */
  Optional<GamificationProfile> initializeUserProfile(String userId);

  /** Get user's gamification profile */
  Optional<GamificationProfile> getUserProfile(String userId);



  /** Award points to user for specific action */
  Optional<GamificationProfile> awardPoints(String userId, String action, int points);

  /** Award points to user for specific activity using enum */
  Optional<GamificationProfile> awardPoints(String userId, GamificationActivity activity);

  /** Process book-related events */
  Optional<GamificationProfile> processBookAdded(String userId);
  Optional<GamificationProfile> processBookRead(String userId);
  Optional<GamificationProfile> processBookFavorited(String userId);
  Optional<GamificationProfile> processBookOfferedForExchange(String userId);

  /** Process exchange-related events */
  Optional<GamificationProfile> processExchangeCreated(String userId);
  Optional<GamificationProfile> processExchangeCompleted(String userId);

  /** Process social events */
  Optional<GamificationProfile> processPostCreated(String userId);
  Optional<GamificationProfile> processCommentCreated(String userId);
  Optional<GamificationProfile> processCommunityJoined(String userId);
  Optional<GamificationProfile> processCommunityCreated(String userId);

  /** Process reading club events */
  Optional<GamificationProfile> processReadingClubJoined(String userId);
  Optional<GamificationProfile> processReadingClubCreated(String userId);

  /** Get all achievements */
  List<Achievement> getAllAchievements();

  /** Get user's achievements */
  List<UserAchievement> getUserAchievements(String userId);

  /** Get unnotified achievements for user */
  List<UserAchievement> getUnnotifiedAchievements(String userId);

  /** Mark achievements as notified */
  void markAchievementsAsNotified(String userId, List<String> achievementIds);

  /** Get all user levels */
  List<UserLevel> getAllUserLevels();

  /** Get level for specific points */
  Optional<UserLevel> getLevelForPoints(int points);

  /** Check and award achievements for user */
  List<UserAchievement> checkAndAwardAchievements(String userId);

  /** Delete all gamification data for user (manual cleanup) */
  boolean deleteUserGamificationData(String userId);
}
