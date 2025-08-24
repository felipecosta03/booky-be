package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.UserBookEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserBookRepository extends JpaRepository<UserBookEntity, String> {

  Optional<UserBookEntity> findByUserIdAndBookId(String userId, String bookId);

  List<UserBookEntity> findByUserId(String userId);

  @Query("SELECT ub FROM UserBookEntity ub WHERE ub.userId = :userId AND ub.favorite = true")
  List<UserBookEntity> findByUserIdAndIsFavoriteTrue(@Param("userId") String userId);

  @Query("SELECT ub FROM UserBookEntity ub WHERE ub.wantsToExchange = true")
  List<UserBookEntity> findByWantsToExchangeTrue();

  @Query("SELECT ub FROM UserBookEntity ub JOIN FETCH ub.book WHERE ub.userId = :userId")
  List<UserBookEntity> findByUserIdWithBook(@Param("userId") String userId);

  @Query(
      "SELECT ub FROM UserBookEntity ub JOIN FETCH ub.book WHERE ub.userId = :userId AND ub.favorite = true")
  List<UserBookEntity> findByUserIdAndIsFavoriteTrueWithBook(@Param("userId") String userId);

  @Query("SELECT ub FROM UserBookEntity ub JOIN FETCH ub.book WHERE ub.wantsToExchange = true")
  List<UserBookEntity> findByWantsToExchangeTrueWithBook();

  @Query(
      "SELECT ub FROM UserBookEntity ub JOIN FETCH ub.book WHERE ub.userId = :userId AND ub.bookId = :bookId")
  Optional<UserBookEntity> findByUserIdAndBookIdWithBook(
      @Param("userId") String userId, @Param("bookId") String bookId);

  boolean existsByUserIdAndBookId(String userId, String bookId);
}
