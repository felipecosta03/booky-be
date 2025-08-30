package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.UserRate;
import java.util.List;
import java.util.Optional;

public interface UserRateService {

    /**
     * Create a new rating for an exchange
     */
    Optional<UserRate> createRating(String exchangeId, String userId, Integer rating, String comment);

    /**
     * Get all ratings for a specific user (ratings received by this user)
     */
    List<UserRate> getUserRatings(String userId);

    /**
     * Get all ratings for a specific exchange
     */
    List<UserRate> getExchangeRatings(String exchangeId);

    /**
     * Check if a user can rate a specific exchange
     */
    boolean canUserRateExchange(String exchangeId, String userId);

    /**
     * Get average rating for a user
     */
    Double getUserAverageRating(String userId);

    /**
     * Get total rating count for a user
     */
    Long getUserRatingCount(String userId);

    /**
     * Check if user has already rated an exchange
     */
    boolean hasUserRatedExchange(String exchangeId, String userId);
}
