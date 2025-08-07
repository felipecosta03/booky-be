package com.uade.bookybe.core.usecase.impl;

import com.uade.bookybe.core.exception.NotFoundException;
import com.uade.bookybe.core.model.BookExchange;
import com.uade.bookybe.core.model.constant.ExchangeStatus;
import com.uade.bookybe.core.usecase.BookExchangeService;
import com.uade.bookybe.infraestructure.entity.BookExchangeEntity;
import com.uade.bookybe.infraestructure.mapper.BookExchangeEntityMapper;
import com.uade.bookybe.infraestructure.repository.BookExchangeRepository;
import com.uade.bookybe.infraestructure.repository.UserBookRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.uade.bookybe.core.model.constant.ExchangeStatus.ACCEPTED;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BookExchangeServiceImpl implements BookExchangeService {

  private final BookExchangeRepository bookExchangeRepository;
  private final UserBookRepository userBookRepository;

  @Override
  public Optional<BookExchange> createExchange(String requesterId, String ownerId, 
                                               List<String> ownerBookIds, List<String> requesterBookIds) {
    log.info("Creating exchange. RequesterId: {}, OwnerId: {}", requesterId, ownerId);

    // Validate that requester and owner are different
    if (requesterId.equals(ownerId)) {
      log.warn("User cannot create exchange with themselves: {}", requesterId);
      return Optional.empty();
    }

    // Validate that all books exist and belong to the respective users
    if (!validateUserBooks(ownerId, ownerBookIds) || !validateUserBooks(requesterId, requesterBookIds)) {
      log.warn("Invalid books provided for exchange");
      return Optional.empty();
    }

    BookExchangeEntity entity = BookExchangeEntity.builder()
        .id("exchange-" + UUID.randomUUID().toString().substring(0, 8))
        .requesterId(requesterId)
        .ownerId(ownerId)
        .status(ExchangeStatus.PENDING)
        .dateCreated(LocalDateTime.now())
        .dateUpdated(LocalDateTime.now())
        .ownerBookIds(ownerBookIds)
        .requesterBookIds(requesterBookIds)
        .build();

    BookExchangeEntity savedEntity = bookExchangeRepository.save(entity);
    return Optional.of(BookExchangeEntityMapper.INSTANCE.toModel(savedEntity));
  }

  @Override
  public List<BookExchange> getUserExchanges(String userId) {
    log.info("Getting all exchanges for user: {}", userId);
    return bookExchangeRepository.findByUserIdOrderByDateCreatedDesc(userId)
        .stream()
        .map(BookExchangeEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public List<BookExchange> getUserExchangesByStatus(String userId, ExchangeStatus status) {
    log.info("Getting exchanges for user: {} with status: {}", userId, status);
    return bookExchangeRepository.findByUserIdAndStatusOrderByDateCreatedDesc(userId, status)
        .stream()
        .map(BookExchangeEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public Optional<BookExchange> getExchangeById(String exchangeId) {
    log.info("Getting exchange by ID: {}", exchangeId);
    return bookExchangeRepository.findByIdWithUsers(exchangeId)
        .map(BookExchangeEntityMapper.INSTANCE::toModel);
  }

  @Override
  public Optional<BookExchange> updateExchangeStatus(String exchangeId, String userId, ExchangeStatus status) {
    log.info("Updating exchange {} status to {} by user {}", exchangeId, status, userId);

    BookExchangeEntity entity = bookExchangeRepository.findById(exchangeId)
        .orElseThrow(() -> new NotFoundException("Exchange not found: " + exchangeId));

    // Validate user permissions based on status
    if (!validateStatusUpdatePermission(entity, userId, status)) {
      log.warn("User {} does not have permission to update exchange {} to status {}", userId, exchangeId, status);
      return Optional.empty();
    }

    entity.setStatus(status);
    entity.setDateUpdated(LocalDateTime.now());

    BookExchangeEntity savedEntity = bookExchangeRepository.save(entity);
    return Optional.of(BookExchangeEntityMapper.INSTANCE.toModel(savedEntity));
  }

  @Override
  public Optional<BookExchange> createCounterOffer(String exchangeId, String userId, 
                                                   List<String> ownerBookIds, List<String> requesterBookIds) {
    log.info("Creating counter offer for exchange: {} by user: {}", exchangeId, userId);

    BookExchangeEntity entity = bookExchangeRepository.findById(exchangeId)
        .orElseThrow(() -> new NotFoundException("Exchange not found: " + exchangeId));

    // Only owner can make counter offers
    if (!entity.getOwnerId().equals(userId)) {
      log.warn("Only owner can make counter offers. UserId: {}, OwnerId: {}", userId, entity.getOwnerId());
      return Optional.empty();
    }

    // Exchange must be in PENDING status to make counter offer
    if (entity.getStatus() != ExchangeStatus.PENDING) {
      log.warn("Cannot make counter offer. Exchange status: {}", entity.getStatus());
      return Optional.empty();
    }

    // Validate books
    if (!validateUserBooks(entity.getOwnerId(), ownerBookIds) || 
        !validateUserBooks(entity.getRequesterId(), requesterBookIds)) {
      log.warn("Invalid books provided for counter offer");
      return Optional.empty();
    }

    entity.setStatus(ExchangeStatus.COUNTERED);
    entity.setOwnerBookIds(ownerBookIds);
    entity.setRequesterBookIds(requesterBookIds);
    entity.setDateUpdated(LocalDateTime.now());

    BookExchangeEntity savedEntity = bookExchangeRepository.save(entity);
    return Optional.of(BookExchangeEntityMapper.INSTANCE.toModel(savedEntity));
  }

  @Override
  public List<BookExchange> getExchangesAsRequester(String userId) {
    log.info("Getting exchanges where user {} is requester", userId);
    return bookExchangeRepository.findByRequesterIdOrderByDateCreatedDesc(userId)
        .stream()
        .map(BookExchangeEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public List<BookExchange> getExchangesAsOwner(String userId) {
    log.info("Getting exchanges where user {} is owner", userId);
    return bookExchangeRepository.findByOwnerIdOrderByDateCreatedDesc(userId)
        .stream()
        .map(BookExchangeEntityMapper.INSTANCE::toModel)
        .collect(Collectors.toList());
  }

  @Override
  public long getPendingExchangesCount(String userId) {
    log.info("Getting pending exchanges count for user: {}", userId);
    long asRequester = bookExchangeRepository.countPendingExchangesByRequester(userId);
    long asOwner = bookExchangeRepository.countPendingExchangesByOwner(userId);
    return asRequester + asOwner;
  }

  private boolean validateUserBooks(String userId, List<String> bookIds) {
    return bookIds.stream()
        .allMatch(bookId -> userBookRepository.existsByUserIdAndBookId(userId, bookId));
  }

  private boolean validateStatusUpdatePermission(BookExchangeEntity entity, String userId, ExchangeStatus status) {
    switch (status) {
      case ACCEPTED:
        return entity.getOwnerId().equals(userId);
      case REJECTED:
        // Only owner can accept or reject
        return entity.getOwnerId().equals(userId);
      case CANCELLED:
        // Only requester can cancel
        return entity.getRequesterId().equals(userId);
      case COMPLETED:
        // Both requester and owner can mark as completed
        return ACCEPTED.equals(entity.getStatus())
            && (entity.getRequesterId().equals(userId) || entity.getOwnerId().equals(userId));
      default:
        return false;
    }
  }
} 