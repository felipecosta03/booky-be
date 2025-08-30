package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.UserRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRateRepository extends JpaRepository<UserRateEntity, String> {

    /**
     * Find all ratings for a specific user (ratings received by this user)
     */
    @Query("SELECT ur FROM UserRateEntity ur WHERE ur.exchangeId IN " +
           "(SELECT be.id FROM BookExchangeEntity be WHERE be.requesterId = :userId OR be.ownerId = :userId) " +
           "AND ur.userId != :userId ORDER BY ur.dateCreated DESC")
    List<UserRateEntity> findRatingsForUser(@Param("userId") String userId);

    /**
     * Find all ratings given by a specific user
     */
    List<UserRateEntity> findByUserIdOrderByDateCreatedDesc(String userId);

    /**
     * Find ratings for a specific exchange
     */
    List<UserRateEntity> findByExchangeIdOrderByDateCreatedDesc(String exchangeId);

    /**
     * Find a specific rating by user and exchange
     */
    Optional<UserRateEntity> findByUserIdAndExchangeId(String userId, String exchangeId);

    /**
     * Check if a user has already rated a specific exchange
     */
    boolean existsByUserIdAndExchangeId(String userId, String exchangeId);

    /**
     * Get average rating for a user (ratings received)
     */
    @Query("SELECT AVG(ur.rating) FROM UserRateEntity ur WHERE ur.exchangeId IN " +
           "(SELECT be.id FROM BookExchangeEntity be WHERE be.requesterId = :userId OR be.ownerId = :userId) " +
           "AND ur.userId != :userId")
    Double getAverageRatingForUser(@Param("userId") String userId);

    /**
     * Count total ratings received by a user
     */
    @Query("SELECT COUNT(ur) FROM UserRateEntity ur WHERE ur.exchangeId IN " +
           "(SELECT be.id FROM BookExchangeEntity be WHERE be.requesterId = :userId OR be.ownerId = :userId) " +
           "AND ur.userId != :userId")
    Long countRatingsForUser(@Param("userId") String userId);
}
