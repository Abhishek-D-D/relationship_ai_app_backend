package com.couplespace.service;

import com.couplespace.dto.CoupleDto;
import com.couplespace.entity.Couple;
import com.couplespace.entity.User;
import com.couplespace.repository.CoupleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CoupleService {

    private final CoupleRepository coupleRepository;
    private final com.couplespace.repository.PartnerPersonaRepository personaRepository;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Transactional
    public CoupleDto createCouple(User creator) {
        // Check if user is already in a couple
        coupleRepository.findByPartnerId(creator.getUserId()).ifPresent(existing -> {
            throw new RuntimeException("You are already in a couple space");
        });

        Couple couple = Couple.builder()
                .partner1(creator)
                .inviteCode(generateInviteCode())
                .build();

        Couple savedCouple = coupleRepository.save(couple);

        // Link Persona if exists
        personaRepository.findByUserId(creator.getUserId()).ifPresent(persona -> {
            persona.setCoupleId(savedCouple.getCoupleId());
            personaRepository.save(persona);
        });

        return CoupleDto.from(savedCouple);
    }

    @Transactional
    public CoupleDto joinCouple(String inviteCode, User joiner) {
        Couple couple = coupleRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Invalid invite code"));

        if (couple.getPartner2() != null) {
            throw new RuntimeException("Couple space is already full");
        }
        if (couple.getPartner1().getUserId().equals(joiner.getUserId())) {
            throw new RuntimeException("You cannot join your own couple space");
        }

        couple.setPartner2(joiner);
        Couple savedCouple = coupleRepository.save(couple);

        // Link Persona if exists
        personaRepository.findByUserId(joiner.getUserId()).ifPresent(persona -> {
            persona.setCoupleId(savedCouple.getCoupleId());
            personaRepository.save(persona);
        });

        return CoupleDto.from(savedCouple);
    }

    @Transactional(readOnly = true)
    public CoupleDto getCoupleForUser(UUID userId) {
        Couple couple = coupleRepository.findByPartnerId(userId)
                .orElseThrow(() -> new RuntimeException("You are not in a couple space yet"));
        return CoupleDto.from(couple);
    }

    @Transactional(readOnly = true)
    public UUID getCoupleIdForUser(UUID userId) {
        return coupleRepository.findByPartnerId(userId)
                .map(Couple::getCoupleId)
                .orElseThrow(() -> new RuntimeException("No couple found for user"));
    }

    @Transactional(readOnly = true)
    public Couple getCoupleById(UUID coupleId) {
        return coupleRepository.findById(coupleId)
                .orElseThrow(() -> new RuntimeException("Couple not found"));
    }

    private String generateInviteCode() {
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    @Transactional(readOnly = true)
    public List<Couple> getAllCompleteCouples() {
        return coupleRepository.findAll().stream()
                .filter(Couple::isComplete)
                .toList();
    }
}
