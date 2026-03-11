package com.couplespace.dto;

import com.couplespace.entity.Couple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CoupleDto(
    UUID coupleId,
    String inviteCode,
    PartnerDto partner1,
    PartnerDto partner2,
    LocalDate anniversaryDate,
    LocalDateTime createdAt,
    boolean isComplete
) {
    public record PartnerDto(UUID userId, String name, String email, String avatarUrl) {}

    public static CoupleDto from(Couple couple) {
        return new CoupleDto(
            couple.getCoupleId(),
            couple.getInviteCode(),
            couple.getPartner1() != null ? new PartnerDto(
                couple.getPartner1().getUserId(),
                couple.getPartner1().getName(),
                couple.getPartner1().getEmail(),
                couple.getPartner1().getAvatarUrl()
            ) : null,
            couple.getPartner2() != null ? new PartnerDto(
                couple.getPartner2().getUserId(),
                couple.getPartner2().getName(),
                couple.getPartner2().getEmail(),
                couple.getPartner2().getAvatarUrl()
            ) : null,
            couple.getAnniversaryDate(),
            couple.getCreatedAt(),
            couple.isComplete()
        );
    }
}
