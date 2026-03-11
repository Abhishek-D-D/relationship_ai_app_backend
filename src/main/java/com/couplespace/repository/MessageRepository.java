package com.couplespace.repository;

import com.couplespace.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByCoupleIdOrderByCreatedAtDesc(UUID coupleId, Pageable pageable);

    List<Message> findByCoupleIdAndCreatedAtBetween(
        UUID coupleId, LocalDateTime start, LocalDateTime end);

    long countByCoupleIdAndIsReadFalseAndSenderIdNot(UUID coupleId, UUID senderId);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true, m.readAt = :now " +
           "WHERE m.coupleId = :coupleId AND m.senderId != :readerId AND m.isRead = false")
    int markAllAsRead(@Param("coupleId") UUID coupleId,
                      @Param("readerId") UUID readerId,
                      @Param("now") LocalDateTime now);
}
