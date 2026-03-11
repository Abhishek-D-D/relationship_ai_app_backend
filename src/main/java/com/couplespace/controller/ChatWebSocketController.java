package com.couplespace.controller;

import com.couplespace.dto.MessageDto;
import com.couplespace.entity.Message;
import com.couplespace.service.CoupleService;
import com.couplespace.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
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
     * Payload: { "content": "Hello!", "messageType": "TEXT", "mediaUrl": null }
     */
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload Map<String, String> payload, Principal principal) {
        try {
            UUID senderId  = UUID.fromString(principal.getName());
            UUID coupleId  = coupleService.getCoupleIdForUser(senderId);
            String content = payload.get("content");
            String typeStr = payload.getOrDefault("messageType", "TEXT");
            String mediaUrl= payload.get("mediaUrl");

            Message.MessageType type;
            try {
                type = Message.MessageType.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                type = Message.MessageType.TEXT;
            }

            MessageDto saved = messageService.saveMessage(coupleId, senderId, content, type, mediaUrl);

            // Broadcast to all subscribers of the couple's topic
            messagingTemplate.convertAndSend("/topic/messages/" + coupleId, saved);

        } catch (Exception e) {
            log.error("Failed to handle WS message: {}", e.getMessage());
        }
    }

    /**
     * Client sends to: /app/chat.typing
     * Payload: { "isTyping": "true" }
     */
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, String> payload, Principal principal) {
        try {
            UUID senderId = UUID.fromString(principal.getName());
            UUID coupleId = coupleService.getCoupleIdForUser(senderId);
            boolean isTyping = Boolean.parseBoolean(payload.getOrDefault("isTyping", "true"));

            messagingTemplate.convertAndSend(
                "/topic/typing/" + coupleId,
                Map.of("userId", senderId.toString(), "isTyping", isTyping)
            );
        } catch (Exception e) {
            log.error("Typing event error: {}", e.getMessage());
        }
    }

    /**
     * Client sends to: /app/chat.presence
     * Payload: { "status": "online" }
     */
    @MessageMapping("/chat.presence")
    public void handlePresence(@Payload Map<String, String> payload, Principal principal) {
        try {
            UUID senderId = UUID.fromString(principal.getName());
            UUID coupleId = coupleService.getCoupleIdForUser(senderId);
            String status = payload.getOrDefault("status", "online");

            messagingTemplate.convertAndSend(
                "/topic/presence/" + coupleId,
                Map.of("userId", senderId.toString(), "status", status)
            );
        } catch (Exception e) {
            log.error("Presence event error: {}", e.getMessage());
        }
    }
}
