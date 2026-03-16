package com.couplespace.repository;

import com.couplespace.entity.MoodSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MoodSnapshotRepository extends JpaRepository<MoodSnapshot, UUID> {
    Optional<MoodSnapshot> findTopByCoupleIdAndUserIdOrderByCreatedAtDesc(UUID coupleId, UUID userId);

    List<MoodSnapshot> findByCoupleIdAndNotificationSentFalse(UUID coupleId);
}
