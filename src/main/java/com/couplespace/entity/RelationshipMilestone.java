package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "relationship_milestones")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipMilestone {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "milestone_id")
    private UUID milestoneId;

    @Column(name = "couple_id", nullable = false)
    private UUID coupleId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "memory_summary", columnDefinition = "TEXT")
    private String memorySummary;

    @Column(name = "milestone_date", nullable = false)
    private LocalDate milestoneDate;

    @Column(name = "milestone_type", length = 50)
    @Builder.Default
    private String milestoneType = "MOMENT";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
