package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mood_snapshots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoodSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "snapshot_id")
    private UUID snapshotId;

    @Column(name = "couple_id", nullable = false)
    private UUID coupleId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String mood = "NEUTRAL";

    @Column(name = "mood_score")
    @Builder.Default
    private int moodScore = 50;

    @Column(name = "mood_note", columnDefinition = "TEXT")
    private String moodNote;

    @Column(name = "notification_sent")
    @Builder.Default
    private boolean notificationSent = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
