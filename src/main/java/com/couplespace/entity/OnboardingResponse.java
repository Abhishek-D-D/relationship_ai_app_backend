package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "onboarding_responses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID coupleId;

    @Column(nullable = false)
    private UUID questionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
