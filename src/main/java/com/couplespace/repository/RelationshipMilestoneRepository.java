package com.couplespace.repository;

import com.couplespace.entity.RelationshipMilestone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RelationshipMilestoneRepository extends JpaRepository<RelationshipMilestone, UUID> {
    List<RelationshipMilestone> findByCoupleIdOrderByMilestoneDateAsc(UUID coupleId);

    long countByCoupleId(UUID coupleId);

    void deleteByCoupleId(UUID coupleId);
}
