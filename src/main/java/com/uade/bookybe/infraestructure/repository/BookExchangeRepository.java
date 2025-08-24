package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.core.model.constant.ExchangeStatus;
import com.uade.bookybe.infraestructure.entity.BookExchangeEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BookExchangeRepository extends JpaRepository<BookExchangeEntity, String> {

  List<BookExchangeEntity> findByRequesterIdOrderByDateCreatedDesc(String requesterId);

  List<BookExchangeEntity> findByOwnerIdOrderByDateCreatedDesc(String ownerId);

  @Query(
      "SELECT be FROM BookExchangeEntity be WHERE (be.requesterId = :userId OR be.ownerId = :userId) ORDER BY be.dateCreated DESC")
  List<BookExchangeEntity> findByUserIdOrderByDateCreatedDesc(@Param("userId") String userId);

  @Query(
      "SELECT be FROM BookExchangeEntity be WHERE (be.requesterId = :userId OR be.ownerId = :userId) AND be.status = :status ORDER BY be.dateCreated DESC")
  List<BookExchangeEntity> findByUserIdAndStatusOrderByDateCreatedDesc(
      @Param("userId") String userId, @Param("status") ExchangeStatus status);

  List<BookExchangeEntity> findByStatusOrderByDateCreatedDesc(ExchangeStatus status);

  @Query(
      "SELECT be FROM BookExchangeEntity be JOIN FETCH be.requester JOIN FETCH be.owner WHERE be.id = :exchangeId")
  Optional<BookExchangeEntity> findByIdWithUsers(@Param("exchangeId") String exchangeId);

  @Query(
      "SELECT be FROM BookExchangeEntity be JOIN FETCH be.requester JOIN FETCH be.owner WHERE (be.requesterId = :userId OR be.ownerId = :userId) ORDER BY be.dateCreated DESC")
  List<BookExchangeEntity> findByUserIdWithUsersOrderByDateCreatedDesc(
      @Param("userId") String userId);

  @Query(
      "SELECT COUNT(be) FROM BookExchangeEntity be WHERE be.requesterId = :userId AND be.status = 'PENDING'")
  long countPendingExchangesByRequester(@Param("userId") String userId);

  @Query(
      "SELECT COUNT(be) FROM BookExchangeEntity be WHERE be.ownerId = :userId AND be.status = 'PENDING'")
  long countPendingExchangesByOwner(@Param("userId") String userId);
}
