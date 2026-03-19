package com.couplespace.controller;

import com.couplespace.dto.MessageDto;
import com.couplespace.dto.MessageReactionDto;
import com.couplespace.entity.Message;
import com.couplespace.entity.User;
import com.couplespace.service.CoupleService;
import com.couplespace.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final CoupleService coupleService;

    /**
     * Client sends to: /app/chat.send
     * Payload: {
     *   "content": "Hello!",
     *   "messageType": "TEXT",
     *   "mediaUrl": null,
     *   "thumbnailUrl": null,
     *   "replyToMessageId": null,
     *   "durationSeconds": null
     * }
     */
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null) {
                log.error("Failed to handle WS message: Authentication is null");
                return;
            }
            User user = (User) authentication.getPrincipal();
            UUID senderId = user.getUserId();
            UUID coupleId = coupleService.getCoupleIdForUser(senderId);

            String content = payload.get("content");
            String typeStr = payload.getOrDefault("messageType", "TEXT");
            String mediaUrl = payload.get("mediaUrl");
            String thumbnailUrl = payload.get("thumbnailUrl");
            String replyToStr = payload.get("replyToMessageId");
            String durationStr = payload.get("durationSeconds");

            UUID replyToMessageId = replyToStr != null && !replyToStr.isBlank() ?
                    UUID.fromString(replyToStr) : null;
            Integer durationSeconds = durationStr != null && !durationStr.isBlank() ?
                    Integer.parseInt(durationStr) : null;

            log.info("Received message from user {}: type={}", senderId, typeStr);

            Message.MessageType type;
            try {
                type = Message.MessageType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid message type {}, defaulting to TEXT", typeStr);
                type = Message.MessageType.TEXT;
            }

            MessageDto saved = messageService.saveMessage(coupleId, senderId, content, type,
                    mediaUrl, replyToMessageId, durationSeconds, thumbnailUrl);
            log.info("Saved message to DB. ID: {}", saved.messageId());

            String destination = "/topic/messages/" + coupleId;
            messagingTemplate.convertAndSend(destination, saved);
            log.info("Broadcast to {}", destination);

        } catch (Exception e) {
            log.error("Failed to handle WS message: {}", e.getMessage(), e);
        }
    }

    /**
     * Client sends to: /app/chat.typing
     * Payload: { "isTyping": "true" }
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null) return;
            User user = (User) authentication.getPrincipal();
            UUID senderId = user.getUserId();
            UUID coupleId = coupleService.getCoupleIdForUser(senderId);
            boolean isTyping = Boolean.parseBoolean(payload.getOrDefault("isTyping", "true"));

            messagingTemplate.convertAndSend(
                    "/topic/typing/" + coupleId,
                    Map.of("userId", senderId.toString(), "isTyping", isTyping));
        } catch (Exception e) {
            log.error("Typing event error: {}", e.getMessage());
        }
    }

    /**
     * Client sends to: /app/chat.presence
     * Payload: { "status": "online" | "offline" | "last_seen_timestamp" }
     */
    @MessageMapping("/chat.presence")
    public void handlePresence(@Payload Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null) return;
            User user = (User) authentication.getPrincipal();
            UUID senderId = user.getUserId();
            UUID coupleId = coupleService.getCoupleIdForUser(senderId);
            String status = payload.getOrDefault("status", "online");

            messagingTemplate.convertAndSend(
                    "/topic/presence/" + coupleId,
                    Map.of("userId", senderId.toString(), "status", status));
        } catch (Exception e) {
            log.error("Presence event error: {}", e.getMessage());
        }
    }

    /**
     * Client sends to: /app/chat.react
     * Payload: { "messageId": "uuid", "emoji": "❤️" }
     */
    @MessageMapping("/chat.react")
    public void handleReaction(@Payload Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null) return;
            User user = (User) authentication.getPrincipal();
            UUID userId = user.getUserId();
            UUID coupleId = coupleService.getCoupleIdForUser(userId);
            UUID messageId = UUID.fromString(payload.get("messageId"));
            String emoji = payload.get("emoji");

            MessageDto updated = messageService.reactToMessage(messageId, userId, emoji);
            log.info("Reaction {} by user {} on message {}", emoji, userId, messageId);

            MessageReactionDto reactionDto = new MessageReactionDto(
                    messageId, userId, user.getName(), emoji, "TOGGLE", updated.reactions());

            messagingTemplate.convertAndSend("/topic/messages/" + coupleId,
                    Map.of("type", "REACTION", "data", reactionDto,
                           "messageId", messageId.toString(),
                           "reactions", updated.reactions()));
        } catch (Exception e) {
            log.error("Reaction event error: {}", e.getMessage(), e);
        }
    }

    /**
     * Client sends to: /app/chat.delete
     * Payload: { "messageId": "uuid", "forEveryone": "true" }
     */
    @MessageMapping("/chat.delete")
    public void handleDelete(@Payload Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null) return;
            User user = (User) authentication.getPrincipal();
            UUID userId = user.getUserId();
            UUID coupleId = coupleService.getCoupleIdForUser(userId);
            UUID messageId = UUID.fromString(payload.get("messageId"));
            boolean forEveryone = Boolean.parseBoolean(payload.getOrDefault("forEveryone", "false"));

            MessageDto updated = messageService.deleteMessage(messageId, userId, forEveryone);

            if (forEveryone) {
                messagingTemplate.convertAndSend("/topic/messages/" + coupleId,
                        Map.of("type", "DELETE", "messageId", messageId.toString(),
                               "forEveryone", "true"));
            }
        } catch (Exception e) {
            log.error("Delete event error: {}", e.getMessage(), e);
        }
    }

    /**
     * Client sends to: /app/chat.read
     * Broadcasts read receipts to partner (blue ticks).
     * Payload: { "messageIds": "id1,id2,..." }
     */
    @MessageMapping("/chat.read")
    public void handleReadReceipt(@Payload Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null) return;
            User user = (User) authentication.getPrincipal();
            UUID userId = user.getUserId();
            UUID coupleId = coupleService.getCoupleIdForUser(userId);

            // Mark all unread messages as read in DB
            messageService.markAsRead(coupleId, userId);

            // Broadcast read event so sender sees blue ticks
            messagingTemplate.convertAndSend("/topic/presence/" + coupleId,
                    Map.of("userId", userId.toString(), "status", "read",
                           "readerId", userId.toString()));
        } catch (Exception e) {
            log.error("Read receipt error: {}", e.getMessage());
        }
    }

    /**
     * Client sends to: /app/chat.delivered
     * Payload: {} (indicates the client has connected and received messages)
     */
    @MessageMapping("/chat.delivered")
    public void handleDelivered(@Payload Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null) return;
            User user = (User) authentication.getPrincipal();
            UUID userId = user.getUserId();
            UUID coupleId = coupleService.getCoupleIdForUser(userId);

            messageService.markAsDelivered(coupleId, userId);

            messagingTemplate.convertAndSend("/topic/presence/" + coupleId,
                    Map.of("userId", userId.toString(), "status", "delivered",
                           "receiverId", userId.toString()));
        } catch (Exception e) {
            log.error("Delivered event error: {}", e.getMessage());
        }
    }

    /**
     * Real-time Call Signaling
     * Client sends to: /app/chat.call
     * Payload: {
     *   "type": "CALL_INVITE" | "CALL_ACCEPT" | "CALL_REJECT" | "CALL_HANGUP",
     *   "callType": "VOICE" | "VIDEO",
     *   "channelName": "couple-uuid",
     *   "callerId": "uuid",
     *   "callerName": "Name"
     * }
     */
    @MessageMapping("/chat.call")
    public void handleCallSignal(@Payload Map<String, String> payload, Authentication authentication) {
        try {
            if (authentication == null) return;
            User user = (User) authentication.getPrincipal();
            UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
            
            log.info("Call Signal: {} from user {} for couple {}", 
                     payload.get("type"), user.getUserId(), coupleId);

            // Broadcast to the couple's call topic
            messagingTemplate.convertAndSend("/topic/calls/" + coupleId, payload);
            
        } catch (Exception e) {
            log.error("Call signaling error: {}", e.getMessage());
        }
    }
}
