package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.PostEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, String> {

  List<PostEntity> findByUserIdOrderByDateCreatedDesc(String userId);

  List<PostEntity> findByCommunityIdOrderByDateCreatedDesc(String communityId);

  @Query("SELECT p FROM PostEntity p WHERE p.communityId IS NULL ORDER BY p.dateCreated DESC")
  List<PostEntity> findGeneralPostsOrderByDateCreatedDesc();

  @Query("SELECT p FROM PostEntity p JOIN FETCH p.user ORDER BY p.dateCreated DESC")
  List<PostEntity> findAllWithUserOrderByDateCreatedDesc();

  @Query(
      "SELECT p FROM PostEntity p WHERE p.communityId = :communityId ORDER BY p.dateCreated DESC")
  List<PostEntity> findByCommunityIdWithUserOrderByDateCreatedDesc(
      @Param("communityId") String communityId);

  // Query para obtener posts de usuarios seguidos (feed del usuario)
  @Query(
      value =
          """
      SELECT p.* FROM post p
      INNER JOIN user_follows uf ON p.user_id = uf.followed_id
      WHERE (uf.follower_id = :userId OR p.user_id = :userId) AND p.community_id IS NULL
      ORDER BY p.date_created DESC
      """,
      nativeQuery = true)
  List<PostEntity> findPostsFromFollowedUsers(@Param("userId") String userId);
}
