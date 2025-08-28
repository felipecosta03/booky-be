package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.core.model.constant.BookStatus;
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

  @Query(
      """
    SELECT ub FROM UserBookEntity ub JOIN FETCH ub.book
    WHERE ub.userId = :userId
    AND (:favorites IS NULL OR ub.favorite = :favorites)
    AND (:status IS NULL OR ub.status = :status)
    AND (:wantsToExchange IS NULL OR ub.wantsToExchange = :wantsToExchange)
    """)
  List<UserBookEntity> findByUserIdWithFilters(
      @Param("userId") String userId,
      @Param("favorites") Boolean favorites,
      @Param("status") BookStatus status,
      @Param("wantsToExchange") Boolean wantsToExchange);

  @Query("SELECT ub FROM UserBookEntity ub JOIN FETCH ub.book WHERE ub.wantsToExchange = true")
  List<UserBookEntity> findByWantsToExchangeTrueWithBook();

  @Query("SELECT ub FROM UserBookEntity ub JOIN FETCH ub.book WHERE ub.book.id IN :bookIds AND ub.userId = :userId ")
  List<UserBookEntity> findByIdInWithBook(@Param("bookIds") List<String> bookIds, @Param("userId") String userId);

  @Query(
      "SELECT ub FROM UserBookEntity ub JOIN FETCH ub.book WHERE ub.userId = :userId AND ub.bookId = :bookId")
  Optional<UserBookEntity> findByUserIdAndBookIdWithBook(
      @Param("userId") String userId, @Param("bookId") String bookId);

  boolean existsByUserIdAndBookId(String userId, String bookId);

  @Query(
      value =
          """
    SELECT u.id, u.username, u.name, u.lastname, u.image
    FROM users u
    INNER JOIN user_books ub ON u.id = ub.user_id
    WHERE ub.book_id IN :bookIds
    AND ub.wants_to_exchange = true
    AND ub.user_id != :excludeUserId
    GROUP BY u.id, u.username, u.name, u.lastname, u.image
    HAVING COUNT(DISTINCT ub.book_id) = :bookCount
    LIMIT 100
    """,
      nativeQuery = true)
  List<Object[]> findUsersByBookIds(
      @Param("bookIds") List<String> bookIds,
      @Param("excludeUserId") String excludeUserId,
      @Param("bookCount") int bookCount);
}
