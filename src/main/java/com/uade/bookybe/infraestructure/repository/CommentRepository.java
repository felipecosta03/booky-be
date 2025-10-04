package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.CommentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, String> {

  List<CommentEntity> findByPostIdOrderByDateCreatedDesc(String postId);

  List<CommentEntity> findByUserIdOrderByDateCreatedDesc(String userId);

  @Query(
      "SELECT c FROM CommentEntity c JOIN FETCH c.user WHERE c.postId = :postId ORDER BY c.dateCreated DESC")
  List<CommentEntity> findByPostIdWithUserOrderByDateCreatedDesc(@Param("postId") String postId);

  @Query("SELECT COUNT(c) FROM CommentEntity c WHERE c.postId = :postId")
  Integer countByPostId(String postId);
}
