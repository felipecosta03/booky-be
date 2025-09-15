package com.uade.bookybe.core.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "scene_image_generations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SceneImageGeneration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "book_id", nullable = false)
  private String bookId;

  @Column(name = "fragment_hash", nullable = false)
  private String fragmentHash;

  @Column(name = "crafted_prompt", columnDefinition = "TEXT", nullable = false)
  private String craftedPrompt;

  @Column(name = "image_url")
  private String imageUrl;

  @Column(name = "image_base64", columnDefinition = "TEXT")
  private String imageBase64;

  @Column(name = "size", nullable = false)
  private String size;

  @Column(name = "style")
  private String style;

  @Column(name = "seed")
  private Integer seed;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "openai_response_time_ms")
  private Long openaiResponseTimeMs;

  @Column(name = "prompt_tokens")
  private Integer promptTokens;

  @Column(name = "total_cost_usd")
  private Double totalCostUsd;
}
