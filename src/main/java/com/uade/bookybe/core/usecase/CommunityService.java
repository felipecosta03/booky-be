package com.uade.bookybe.core.usecase;

import com.uade.bookybe.core.model.Community;
import java.util.List;
import java.util.Optional;

public interface CommunityService {
  
  Optional<Community> createCommunity(String adminId, String name, String description);
  
  Optional<Community> getCommunityById(String communityId);
  
  List<Community> getCommunitiesByAdminId(String adminId);
  
  List<Community> getAllCommunities();
  
  List<Community> searchCommunities(String query);
  
  Optional<Community> updateCommunity(String communityId, String adminId, String name, String description);
  
  boolean deleteCommunity(String communityId, String adminId);
  
  boolean joinCommunity(String communityId, String userId);
  
  boolean leaveCommunity(String communityId, String userId);
  
  List<Community> getUserCommunities(String userId);
  
  boolean isUserMember(String communityId, String userId);
} 