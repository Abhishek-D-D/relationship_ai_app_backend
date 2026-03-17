package com.couplespace.dto;

import com.couplespace.entity.Message;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageDto(
    UUID messageId,
    UUID coupleId,
    UUID senderId,
    String senderName,
    String messageType,
    String content,
    String mediaUrl,
    String thumbnailUrl,
    UUID replyToMessageId,
    String replyToContent,
    String replyToSenderName,
    String reactions,
    Integer durationSeconds,
    boolean isRead,
    boolean isDeleted,
    boolean deletedForEveryone,
    boolean isStarred,
    LocalDateTime readAt,
    LocalDateTime deliveredAt,
    LocalDateTime editedAt,
    LocalDateTime createdAt
) {
    public static MessageDto from(Message message, String senderName) {
        return new MessageDto(
            message.getMessageId(),
            message.getCoupleId(),
            message.getSenderId(),
            senderName,
            message.getMessageType().name(),
            message.isDeleted() && message.isDeletedForEveryone() ? null : message.getContent(),
            message.isDeleted() && message.isDeletedForEveryone() ? null : message.getMediaUrl(),
            message.isDeleted() && message.isDeletedForEveryone() ? null : message.getThumbnailUrl(),
            message.getReplyToMessageId(),
            null, // replyToContent — resolved by service
            null, // replyToSenderName — resolved by service
            message.getReactions() != null ? message.getReactions() : "[]",
            message.getDurationSeconds(),
            message.isRead(),
            message.isDeleted(),
            message.isDeletedForEveryone(),
            message.isStarred(),
            message.getReadAt(),
            message.getDeliveredAt(),
            message.getEditedAt(),
            message.getCreatedAt()
        );
    }

    public static MessageDto from(Message message, String senderName, String replyToContent, String replyToSenderName) {
        return new MessageDto(
            message.getMessageId(),
            message.getCoupleId(),
            message.getSenderId(),
            senderName,
            message.getMessageType().name(),
            message.isDeleted() && message.isDeletedForEveryone() ? null : message.getContent(),
            message.isDeleted() && message.isDeletedForEveryone() ? null : message.getMediaUrl(),
            message.isDeleted() && message.isDeletedForEveryone() ? null : message.getThumbnailUrl(),
            message.getReplyToMessageId(),
            replyToContent,
            replyToSenderName,
            message.getReactions() != null ? message.getReactions() : "[]",
            message.getDurationSeconds(),
            message.isRead(),
            message.isDeleted(),
            message.isDeletedForEveryone(),
            message.isStarred(),
            message.getReadAt(),
            message.getDeliveredAt(),
            message.getEditedAt(),
            message.getCreatedAt()
        );
    }
}
