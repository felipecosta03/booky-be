package com.uade.bookybe.infraestructure.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_rates")
public class UserRateEntity {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "exchange_id", nullable = false)
    private String exchangeId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", insertable = false, updatable = false)
    private BookExchangeEntity exchange;
}
