package com.couplespace.repository;

import com.couplespace.entity.OnboardingResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OnboardingResponseRepository extends JpaRepository<OnboardingResponse, UUID> {
    List<OnboardingResponse> findByCoupleId(UUID coupleId);

    List<OnboardingResponse> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
