package com.uade.bookybe.core.model.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum GamificationActivity {
  
  // Book-related activities
  BOOK_ADDED(10, "Agregar libro a biblioteca"),
  BOOK_READ(25, "Marcar libro como leído"),
  BOOK_FAVORITED(5, "Marcar libro como favorito"),
  BOOK_OFFERED_FOR_EXCHANGE(15, "Ofrecer libro para intercambio"),
  
  // Exchange-related activities
  EXCHANGE_CREATED(20, "Crear solicitud de intercambio"),
  EXCHANGE_COMPLETED(50, "Completar intercambio exitoso"),
  
  // Social activities
  POST_CREATED(15, "Crear post"),
  COMMENT_CREATED(10, "Crear comentario"),
  
  // Community activities
  COMMUNITY_JOINED(20, "Unirse a comunidad"),
  COMMUNITY_CREATED(100, "Crear comunidad"),
  
  // Reading club activities
  READING_CLUB_JOINED(25, "Unirse a club de lectura"),
  READING_CLUB_CREATED(75, "Crear club de lectura");
  
  private final int points;
  private final String description;
}
