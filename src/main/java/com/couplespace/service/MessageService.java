package com.couplespace.service;

import com.couplespace.dto.MessageDto;
import com.couplespace.entity.Message;
import com.couplespace.entity.User;
import com.couplespace.repository.MessageRepository;
import com.couplespace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<MessageDto> getMessages(UUID coupleId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Message> messages = messageRepository.findByCoupleIdOrderByCreatedAtDesc(coupleId, pageable);

        // Bulk load sender names
        List<UUID> senderIds = messages.stream()
                .map(Message::getSenderId).distinct().collect(Collectors.toList());
        Map<UUID, String> senderNames = userRepository.findAllById(senderIds).stream()
                .collect(Collectors.toMap(User::getUserId, User::getName));

        return messages.stream()
                .map(m -> MessageDto.from(m, senderNames.getOrDefault(m.getSenderId(), "Unknown")))
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageDto saveMessage(UUID coupleId, UUID senderId,
                                   String content, Message.MessageType type,
                                   String mediaUrl) {
        Message message = Message.builder()
                .coupleId(coupleId)
                .senderId(senderId)
                .content(content)
                .messageType(type)
                .mediaUrl(mediaUrl)
                .build();
        message = messageRepository.save(message);

        String senderName = userRepository.findById(senderId)
                .map(User::getName).orElse("Unknown");
        return MessageDto.from(message, senderName);
    }

    @Transactional
    public int markAsRead(UUID coupleId, UUID readerId) {
        return messageRepository.markAllAsRead(coupleId, readerId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID coupleId, UUID userId) {
        return messageRepository.countByCoupleIdAndIsReadFalseAndSenderIdNot(coupleId, userId);
    }
}
