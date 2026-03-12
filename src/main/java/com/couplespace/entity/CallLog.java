package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "call_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallLog {

    public enum CallType {
        VOICE, VIDEO
    }

    public enum CallStatus {
        COMPLETED, MISSED, DECLINED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "call_id")
    private UUID callId;

    @Column(name = "couple_id", nullable = false)
    private UUID coupleId;

    @Column(name = "initiator_id", nullable = false)
    private UUID initiatorId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "call_type", nullable = false)
    private CallType callType;

    @Column(name = "duration_seconds", nullable = false)
    @Builder.Default
    private int durationSeconds = 0;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false)
    @Builder.Default
    private CallStatus status = CallStatus.COMPLETED;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;
}
