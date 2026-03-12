package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "onboarding_questions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String questionText;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "JSONB")
    private String options; // Stored as a JSON string

    private Integer weight;
}
