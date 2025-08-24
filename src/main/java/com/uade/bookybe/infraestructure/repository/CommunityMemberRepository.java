package com.uade.bookybe.infraestructure.repository;

import com.uade.bookybe.infraestructure.entity.CommunityMemberEntity;
import com.uade.bookybe.infraestructure.entity.CommunityMemberId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityMemberRepository
    extends JpaRepository<CommunityMemberEntity, CommunityMemberId> {

  List<CommunityMemberEntity> findByCommunityId(String communityId);

  List<CommunityMemberEntity> findByUserId(String userId);

  @Query(
      "SELECT cm FROM CommunityMemberEntity cm JOIN FETCH cm.user WHERE cm.communityId = :communityId")
  List<CommunityMemberEntity> findByCommunityIdWithUser(@Param("communityId") String communityId);

  @Query(
      "SELECT cm FROM CommunityMemberEntity cm JOIN FETCH cm.community WHERE cm.userId = :userId")
  List<CommunityMemberEntity> findByUserIdWithCommunity(@Param("userId") String userId);

  boolean existsByCommunityIdAndUserId(String communityId, String userId);

  long countByCommunityId(String communityId);
}
