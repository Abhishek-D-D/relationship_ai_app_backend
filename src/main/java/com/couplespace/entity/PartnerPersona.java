package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "partner_personas")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerPersona {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "persona_id")
    private UUID personaId;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "couple_id", nullable = false)
    private UUID coupleId;

    @Column(name = "communication_style")
    private String communicationStyle; // e.g., "The Wordsmith", "The Deep Thinker"

    @Column(name = "love_language_primary")
    private String primaryLoveLanguage;

    @Column(name = "aura")
    private String aura; // A short visual/vibe description

    @Column(columnDefinition = "TEXT")
    private String traits; // JSON or comma-separated list of traits

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
