package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_coach_messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCoachMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID messageId;

    @Column(nullable = false)
    private UUID coupleId;

    @Column(nullable = false)
    private UUID userId; // The user who "owns" this chat thread

    private UUID senderId; // Null for AI messages, non-null for user messages

    @Column(nullable = false)
    private String role; // "USER" or "ASSISTANT"

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
