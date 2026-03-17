package com.couplespace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "messages")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    public enum MessageType {
        TEXT, IMAGE, AUDIO, VIDEO, VIDEO_NOTE, STICKER, DOCUMENT, LOCATION, CONTACT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "couple_id", nullable = false)
    private UUID coupleId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.TEXT;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "media_url")
    private String mediaUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Column(name = "reply_to_message_id")
    private UUID replyToMessageId;

    // JSON string: [{"userId":"...","emoji":"❤️"}, ...]
    @Column(name = "reactions", columnDefinition = "TEXT")
    @Builder.Default
    private String reactions = "[]";

    // Duration in seconds for AUDIO / VIDEO / VIDEO_NOTE
    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "is_read")
    @Builder.Default
    private boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_for_everyone")
    @Builder.Default
    private boolean deletedForEveryone = false;

    @Column(name = "is_starred")
    @Builder.Default
    private boolean isStarred = false;

    @Column(name = "edited_at")
    private LocalDateTime editedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
