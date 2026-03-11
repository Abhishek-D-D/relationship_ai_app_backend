package com.couplespace.repository;

import com.couplespace.entity.RelationshipMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RelationshipMetricsRepository extends JpaRepository<RelationshipMetrics, UUID> {
    Optional<RelationshipMetrics> findTopByCoupleIdOrderByPeriodStartDesc(UUID coupleId);
}
