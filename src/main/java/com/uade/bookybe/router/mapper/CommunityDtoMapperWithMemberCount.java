package com.uade.bookybe.router.mapper;

import com.uade.bookybe.core.model.Community;
import com.uade.bookybe.infraestructure.repository.CommunityMemberRepository;
import com.uade.bookybe.router.dto.community.CommunityDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Mapper personalizado para CommunityDto que incluye el cálculo dinámico del memberCount.
 * Esta clase extiende la funcionalidad del mapper básico agregando la consulta de miembros.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CommunityDtoMapperWithMemberCount {
  
  private final CommunityMemberRepository communityMemberRepository;
  
  /**
   * Convierte un modelo Community a CommunityDto incluyendo el memberCount calculado dinámicamente.
   * 
   * @param model El modelo Community a convertir
   * @return CommunityDto con memberCount calculado, o null si el modelo es null
   */
  public CommunityDto toDtoWithMemberCount(Community model) {
    if (model == null) {
      return null;
    }
    
    // Usar el mapper básico para la conversión inicial
    CommunityDto dto = CommunityDtoMapper.INSTANCE.toDto(model);
    
    // Calcular memberCount dinámicamente
    if (model.getId() != null) {
      try {
        long memberCount = communityMemberRepository.countByCommunityId(model.getId());
        dto.setMemberCount(memberCount);
        log.debug("Community {} has {} members", model.getId(), memberCount);
      } catch (Exception e) {
        log.warn("Error calculating member count for community {}: {}", model.getId(), e.getMessage());
        dto.setMemberCount(0);
      }
    } else {
      dto.setMemberCount(0);
    }
    
    return dto;
  }
}
