package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "relationship_metrics")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class RelationshipMetrics {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "metric_id")
    private UUID metricId;

    @Column(name = "couple_id", nullable = false)
    private UUID coupleId;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "message_count")
    @Builder.Default
    private int messageCount = 0;

    @Column(name = "avg_response_time_mins", precision = 8, scale = 2)
    @Builder.Default
    private BigDecimal avgResponseTimeMins = BigDecimal.ZERO;

    @Column(name = "conflict_score")
    @Builder.Default
    private int conflictScore = 0;

    @Column(name = "positive_score")
    @Builder.Default
    private int positiveScore = 50;

    @CreationTimestamp
    @Column(name = "computed_at", updatable = false)
    private LocalDateTime computedAt;
}
