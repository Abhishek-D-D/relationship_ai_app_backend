package com.couplespace.controller;

import com.couplespace.dto.ApiResponse;
import com.couplespace.dto.MessageDto;
import com.couplespace.entity.User;
import com.couplespace.service.CoupleService;
import com.couplespace.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final CoupleService coupleService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(messageService.getMessages(coupleId, page, size)));
    }

    @PostMapping("/read")
    public ResponseEntity<ApiResponse<Integer>> markRead(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        int count = messageService.markAsRead(coupleId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(count + " messages marked as read", count));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        long count = messageService.getUnreadCount(coupleId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(count));
    }

    @PostMapping("/{messageId}/react")
    public ResponseEntity<ApiResponse<MessageDto>> reactToMessage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID messageId,
            @RequestBody Map<String, String> body) {
        String emoji = body.get("emoji");
        MessageDto updated = messageService.reactToMessage(messageId, user.getUserId(), emoji);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<MessageDto>> deleteMessage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID messageId,
            @RequestParam(defaultValue = "false") boolean forEveryone) {
        MessageDto updated = messageService.deleteMessage(messageId, user.getUserId(), forEveryone);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @PutMapping("/{messageId}/star")
    public ResponseEntity<ApiResponse<MessageDto>> starMessage(
            @AuthenticationPrincipal User user,
            @PathVariable UUID messageId) {
        MessageDto updated = messageService.starMessage(messageId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<MessageDto>>> searchMessages(
            @AuthenticationPrincipal User user,
            @RequestParam String q) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        List<MessageDto> results = messageService.searchMessages(coupleId, q);
        return ResponseEntity.ok(ApiResponse.ok(results));
    }
}
