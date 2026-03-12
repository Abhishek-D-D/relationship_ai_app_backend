package com.couplespace.repository;

import com.couplespace.entity.AiCoachMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiCoachMessageRepository extends JpaRepository<AiCoachMessage, UUID> {
    List<AiCoachMessage> findTop20ByCoupleIdOrderByCreatedAtAsc(UUID coupleId);
}
