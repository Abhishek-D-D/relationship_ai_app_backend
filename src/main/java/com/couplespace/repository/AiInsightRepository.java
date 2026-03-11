package com.couplespace.repository;

import com.couplespace.entity.AiInsight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiInsightRepository extends JpaRepository<AiInsight, UUID> {
    Optional<AiInsight> findTopByCoupleIdOrderByWeekStartDesc(UUID coupleId);
}
