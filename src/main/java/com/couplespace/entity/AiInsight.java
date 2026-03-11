package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_insights")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class AiInsight {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "insight_id")
    private UUID insightId;

    @Column(name = "couple_id", nullable = false)
    private UUID coupleId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "health_score", nullable = false)
    private int healthScore;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String suggestions;

    @CreationTimestamp
    @Column(name = "generated_at", updatable = false)
    private LocalDateTime generatedAt;
}
