package com.couplespace.repository;

import com.couplespace.entity.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, UUID> {
    List<CallLog> findByCoupleIdAndStartedAtBetween(UUID coupleId, LocalDateTime start, LocalDateTime end);
}
