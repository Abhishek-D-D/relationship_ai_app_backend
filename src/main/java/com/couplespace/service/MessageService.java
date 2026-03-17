package com.couplespace.service;

import com.couplespace.dto.MessageDto;
import com.couplespace.entity.Message;
import com.couplespace.entity.User;
import com.couplespace.repository.MessageRepository;
import com.couplespace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(UUID coupleId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messages = messageRepository.findByCoupleIdOrderByCreatedAtDesc(coupleId, pageable);

        List<UUID> senderIds = messages.stream()
                .map(Message::getSenderId).distinct().collect(Collectors.toList());
        Map<UUID, String> senderNames = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));

        // Collect all replyToMessageIds to resolve in bulk
        List<UUID> replyIds = messages.stream()
                .map(Message::getReplyToMessageId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<UUID, Message> replyMessages = replyIds.isEmpty() ? new HashMap<>() :
                messageRepository.findAllById(replyIds).stream()
                        .collect(Collectors.toMap(Message::getMessageId, m -> m));
        Map<UUID, String> replySenderNames = replyMessages.values().stream()
                .map(Message::getSenderId).distinct()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> senderNames.getOrDefault(id,
                                userRepository.findById(id).map(User::getName).orElse("Unknown"))
                ));

        return messages.stream().map(m -> {
            String senderName = senderNames.getOrDefault(m.getSenderId(), "Unknown");
            if (m.getReplyToMessageId() != null && replyMessages.containsKey(m.getReplyToMessageId())) {
                Message reply = replyMessages.get(m.getReplyToMessageId());
                String replySender = replySenderNames.getOrDefault(reply.getSenderId(), "Unknown");
                String replyContent = reply.isDeleted() ? "This message was deleted" : reply.getContent();
                return MessageDto.from(m, senderName, replyContent, replySender);
            }
            return MessageDto.from(m, senderName);
        }).collect(Collectors.toList());
    }

    @Transactional
    public MessageDto saveMessage(UUID coupleId, UUID senderId,
                                   String content, Message.MessageType type,
                                   String mediaUrl) {
        return saveMessage(coupleId, senderId, content, type, mediaUrl, null, null, null);
    }

    @Transactional
    public MessageDto saveMessage(UUID coupleId, UUID senderId,
                                   String content, Message.MessageType type,
                                   String mediaUrl, UUID replyToMessageId,
                                   Integer durationSeconds, String thumbnailUrl) {
        Message message = Message.builder()
                .coupleId(coupleId)
                .senderId(senderId)
                .content(content)
                .messageType(type)
                .mediaUrl(mediaUrl)
                .thumbnailUrl(thumbnailUrl)
                .replyToMessageId(replyToMessageId)
                .durationSeconds(durationSeconds)
                .build();
        message = messageRepository.save(message);

        String senderName = userRepository.findById(senderId)
                .map(User::getName).orElse("Unknown");

        if (replyToMessageId != null) {
            Optional<Message> replyMsg = messageRepository.findById(replyToMessageId);
            if (replyMsg.isPresent()) {
                Message reply = replyMsg.get();
                String replySenderName = userRepository.findById(reply.getSenderId())
                        .map(User::getName).orElse("Unknown");
                String replyContent = reply.isDeleted() ? "This message was deleted" : reply.getContent();
                return MessageDto.from(message, senderName, replyContent, replySenderName);
            }
        }
        return MessageDto.from(message, senderName);
    }

    @Transactional
    public int markAsRead(UUID coupleId, UUID readerId) {
        return messageRepository.markAllAsRead(coupleId, readerId, LocalDateTime.now());
    }

    @Transactional
    public int markAsDelivered(UUID coupleId, UUID receiverId) {
        return messageRepository.markAllAsDelivered(coupleId, receiverId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID coupleId, UUID userId) {
        return messageRepository.countByCoupleIdAndIsReadFalseAndSenderIdNot(coupleId, userId);
    }

    /**
     * Toggle emoji reaction on a message.
     * Reactions stored as JSON array: [{"userId":"...","emoji":"❤️"}, ...]
     */
    @Transactional
    public MessageDto reactToMessage(UUID messageId, UUID userId, String emoji) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        String reactionsJson = message.getReactions() != null ? message.getReactions() : "[]";

        // Simple string-manipulation approach to avoid Jackson dependency issues
        String userIdStr = userId.toString();
        String entry = "{\"userId\":\"" + userIdStr + "\",\"emoji\":\"" + emoji + "\"}";

        if (reactionsJson.contains("\"userId\":\"" + userIdStr + "\",\"emoji\":\"" + emoji + "\"")) {
            // Remove existing reaction (toggle off)
            reactionsJson = reactionsJson.replace("," + entry, "")
                    .replace(entry + ",", "")
                    .replace(entry, "");
        } else {
            // Remove any existing reaction from this user (swap emoji)
            if (reactionsJson.contains("\"userId\":\"" + userIdStr + "\"")) {
                reactionsJson = reactionsJson.replaceAll(
                        "\\{\"userId\":\"" + userIdStr + "\",\"emoji\":\"[^\"]+\"\\}", "");
                reactionsJson = reactionsJson.replace(",,", ",")
                        .replace("[,", "[").replace(",]", "]");
            }
            // Add new reaction
            if (reactionsJson.equals("[]")) {
                reactionsJson = "[" + entry + "]";
            } else {
                reactionsJson = reactionsJson.substring(0, reactionsJson.length() - 1) + "," + entry + "]";
            }
        }

        message.setReactions(reactionsJson);
        messageRepository.save(message);

        String senderName = userRepository.findById(message.getSenderId())
                .map(User::getName).orElse("Unknown");
        return MessageDto.from(message, senderName);
    }

    /**
     * Soft-delete a message.
     * If forEveryone=true, content/media is hidden for both parties.
     */
    @Transactional
    public MessageDto deleteMessage(UUID messageId, UUID requesterId, boolean forEveryone) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getSenderId().equals(requesterId) && forEveryone) {
            throw new RuntimeException("Only sender can delete for everyone");
        }

        message.setDeleted(true);
        message.setDeletedAt(LocalDateTime.now());
        message.setDeletedForEveryone(forEveryone);
        messageRepository.save(message);

        String senderName = userRepository.findById(message.getSenderId())
                .map(User::getName).orElse("Unknown");
        return MessageDto.from(message, senderName);
    }

    /**
     * Toggle star on a message.
     */
    @Transactional
    public MessageDto starMessage(UUID messageId, UUID userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setStarred(!message.isStarred());
        messageRepository.save(message);
        String senderName = userRepository.findById(message.getSenderId())
                .map(User::getName).orElse("Unknown");
        return MessageDto.from(message, senderName);
    }

    /**
     * Search messages by content (case-insensitive).
     */
    @Transactional(readOnly = true)
    public List<MessageDto> searchMessages(UUID coupleId, String query) {
        List<Message> results = messageRepository
                .findByCoupleIdAndContentContainingIgnoreCaseAndIsDeletedFalse(coupleId, query);

        List<UUID> senderIds = results.stream()
                .map(Message::getSenderId).distinct().collect(Collectors.toList());
        Map<UUID, String> senderNames = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));

        return results.stream()
                .map(m -> MessageDto.from(m, senderNames.getOrDefault(m.getSenderId(), "Unknown")))
                .collect(Collectors.toList());
    }
}
