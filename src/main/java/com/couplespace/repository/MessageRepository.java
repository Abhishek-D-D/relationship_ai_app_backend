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

        @Modifying
        @Query("UPDATE Message m SET m.deliveredAt = :now " +
                        "WHERE m.coupleId = :coupleId AND m.senderId != :receiverId AND m.deliveredAt IS NULL")
        int markAllAsDelivered(@Param("coupleId") UUID coupleId,
                        @Param("receiverId") UUID receiverId,
                        @Param("now") LocalDateTime now);

        void deleteBySenderId(UUID senderId);

        // Used by TimelineService: fetch oldest-first for context building
        @Query("SELECT m FROM Message m WHERE m.coupleId = :coupleId AND m.content IS NOT NULL ORDER BY m.createdAt ASC")
        List<Message> findByCoupleIdOrderByCreatedAtAsc(@Param("coupleId") UUID coupleId,
                        org.springframework.data.domain.Pageable pageable);

        // Used by MoodAnalysisService: last N hours of messages for a specific sender
        @Query("SELECT m FROM Message m WHERE m.coupleId = :coupleId AND m.senderId = :senderId AND m.createdAt >= :since AND m.content IS NOT NULL ORDER BY m.createdAt DESC")
        List<Message> findRecentBySender(@Param("coupleId") UUID coupleId, @Param("senderId") UUID senderId,
                        @Param("since") LocalDateTime since);

        // Search messages by content
        List<Message> findByCoupleIdAndContentContainingIgnoreCaseAndIsDeletedFalse(UUID coupleId, String content);

        // Get starred messages
        List<Message> findByCoupleIdAndIsStarredTrue(UUID coupleId);
}
