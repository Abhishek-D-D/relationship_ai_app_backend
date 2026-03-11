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
    boolean isRead,
    LocalDateTime readAt,
    LocalDateTime createdAt
) {
    public static MessageDto from(Message message, String senderName) {
        return new MessageDto(
            message.getMessageId(),
            message.getCoupleId(),
            message.getSenderId(),
            senderName,
            message.getMessageType().name(),
            message.getContent(),
            message.getMediaUrl(),
            message.isRead(),
            message.getReadAt(),
            message.getCreatedAt()
        );
    }
}
