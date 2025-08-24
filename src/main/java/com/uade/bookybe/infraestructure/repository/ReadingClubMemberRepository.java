package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.ReadingClubMemberEntity;
import com.uade.bookybe.infraestructure.entity.ReadingClubMemberId;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReadingClubMemberRepository
    extends JpaRepository<ReadingClubMemberEntity, ReadingClubMemberId> {

  List<ReadingClubMemberEntity> findByReadingClubId(String readingClubId);

  List<ReadingClubMemberEntity> findByUserId(String userId);

  @Query(
      "SELECT rcm FROM ReadingClubMemberEntity rcm JOIN FETCH rcm.user WHERE rcm.readingClubId = :readingClubId")
  List<ReadingClubMemberEntity> findByReadingClubIdWithUser(
      @Param("readingClubId") String readingClubId);

  @Query(
      "SELECT rcm FROM ReadingClubMemberEntity rcm JOIN FETCH rcm.readingClub WHERE rcm.userId = :userId")
  List<ReadingClubMemberEntity> findByUserIdWithReadingClub(@Param("userId") String userId);

  boolean existsByReadingClubIdAndUserId(String readingClubId, String userId);

  @Transactional
  @Modifying
  @Query(
      "DELETE FROM ReadingClubMemberEntity rcm WHERE rcm.readingClubId = :readingClubId AND rcm.userId = :userId")
  void leaveFromReadingClub(String readingClubId, String userId);

  @Transactional
  @Modifying
  @Query("DELETE FROM ReadingClubMemberEntity rcm WHERE rcm.readingClubId = :readingClubId")
  void deleteAllByReadingClubId(@Param("readingClubId") String readingClubId);

  long countByReadingClubId(String readingClubId);
}
