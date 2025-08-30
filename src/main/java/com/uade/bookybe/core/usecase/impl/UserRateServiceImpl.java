package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.BadRequestException;
import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.UserRate;
import com.uade.bookybe.core.model.constant.ExchangeStatus;
import com.uade.bookybe.core.usecase.UserRateService;
import com.uade.bookybe.infraestructure.entity.BookExchangeEntity;
import com.uade.bookybe.infraestructure.entity.UserRateEntity;
import com.uade.bookybe.infraestructure.mapper.UserRateEntityMapper;
import com.uade.bookybe.infraestructure.repository.BookExchangeRepository;
import com.uade.bookybe.infraestructure.repository.UserRateRepository;
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
public class UserRateServiceImpl implements UserRateService {

    private final UserRateRepository userRateRepository;
    private final BookExchangeRepository bookExchangeRepository;
    private final UserRateEntityMapper userRateEntityMapper = UserRateEntityMapper.INSTANCE;

    @Override
    @Transactional
    public Optional<UserRate> createRating(String exchangeId, String userId, Integer rating, String comment) {
        log.info("Creating rating for exchange {} by user {} with rating {}", exchangeId, userId, rating);

        // Validate rating value
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }

        // Check if exchange exists and is completed
        Optional<BookExchangeEntity> exchangeOpt = bookExchangeRepository.findById(exchangeId);
        if (exchangeOpt.isEmpty()) {
            throw new NotFoundException("Exchange not found");
        }

        BookExchangeEntity exchange = exchangeOpt.get();
        
        // Verify exchange is completed
        if (exchange.getStatus() != ExchangeStatus.COMPLETED) {
            throw new BadRequestException("Can only rate completed exchanges");
        }

        // Verify user is part of the exchange
        if (!exchange.getRequesterId().equals(userId) && !exchange.getOwnerId().equals(userId)) {
            throw new BadRequestException("User is not part of this exchange");
        }

        // Check if user has already rated this exchange
        if (userRateRepository.existsByUserIdAndExchangeId(userId, exchangeId)) {
            throw new BadRequestException("User has already rated this exchange");
        }

        // Create the rating
        UserRateEntity ratingEntity = UserRateEntity.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .exchangeId(exchangeId)
                .rating(rating)
                .comment(comment)
                .dateCreated(LocalDateTime.now())
                .build();

        UserRateEntity savedEntity = userRateRepository.save(ratingEntity);
        UserRate savedRating = userRateEntityMapper.toModel(savedEntity);

        log.info("Rating created successfully with ID: {}", savedEntity.getId());
        return Optional.of(savedRating);
    }

    @Override
    public List<UserRate> getUserRatings(String userId) {
        log.info("Getting ratings for user: {}", userId);
        
        List<UserRateEntity> ratingEntities = userRateRepository.findRatingsForUser(userId);
        return ratingEntities.stream()
                .map(userRateEntityMapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserRate> getExchangeRatings(String exchangeId) {
        log.info("Getting ratings for exchange: {}", exchangeId);
        
        List<UserRateEntity> ratingEntities = userRateRepository.findByExchangeIdOrderByDateCreatedDesc(exchangeId);
        return ratingEntities.stream()
                .map(userRateEntityMapper::toModel)
                .collect(Collectors.toList());
    }

    @Override
    public boolean canUserRateExchange(String exchangeId, String userId) {
        log.debug("Checking if user {} can rate exchange {}", userId, exchangeId);

        // Check if exchange exists and is completed
        Optional<BookExchangeEntity> exchangeOpt = bookExchangeRepository.findById(exchangeId);
        if (exchangeOpt.isEmpty()) {
            return false;
        }

        BookExchangeEntity exchange = exchangeOpt.get();
        
        // Must be completed
        if (exchange.getStatus() != ExchangeStatus.COMPLETED) {
            return false;
        }

        // User must be part of the exchange
        if (!exchange.getRequesterId().equals(userId) && !exchange.getOwnerId().equals(userId)) {
            return false;
        }

        // User must not have already rated
        return !userRateRepository.existsByUserIdAndExchangeId(userId, exchangeId);
    }

    @Override
    public Double getUserAverageRating(String userId) {
        log.debug("Getting average rating for user: {}", userId);
        return userRateRepository.getAverageRatingForUser(userId);
    }

    @Override
    public Long getUserRatingCount(String userId) {
        log.debug("Getting rating count for user: {}", userId);
        return userRateRepository.countRatingsForUser(userId);
    }

    @Override
    public boolean hasUserRatedExchange(String exchangeId, String userId) {
        return userRateRepository.existsByUserIdAndExchangeId(userId, exchangeId);
    }
}
