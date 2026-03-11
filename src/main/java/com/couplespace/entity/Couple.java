package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "couples")
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class Couple {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "couple_id")
    private UUID coupleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner1_id", nullable = false)
    private User partner1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "partner2_id")
    private User partner2;

    @Column(name = "invite_code", nullable = false, unique = true, length = 12)
    private String inviteCode;

    @Column(name = "anniversary_date")
    private LocalDate anniversaryDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isComplete() {
        return partner1 != null && partner2 != null;
    }
}
