package com.couplespace.controller;

import com.couplespace.dto.ApiResponse;
import com.couplespace.entity.AiInsight;
import com.couplespace.entity.User;
import com.couplespace.entity.PartnerPersona;
import com.couplespace.repository.AiInsightRepository;
import com.couplespace.repository.PartnerPersonaRepository;
import com.couplespace.service.AiCoachService;
import com.couplespace.service.CoupleService;
import com.couplespace.entity.AiCoachMessage;
import com.couplespace.repository.AiCoachMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiCoachService aiCoachService;
    private final AiInsightRepository insightRepository;
    private final PartnerPersonaRepository personaRepository;
    private final AiCoachMessageRepository coachMessageRepository;
    private final CoupleService coupleService;

    @PostMapping("/coach/chat")
    public ResponseEntity<ApiResponse<Map<String, String>>> chat(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String message = body.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("message is required"));
        }
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        String reply = aiCoachService.chat(coupleId, user.getUserId(), message);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("reply", reply)));
    }

    @GetMapping("/coach/history")
    public ResponseEntity<ApiResponse<java.util.List<AiCoachMessage>>> getCoachHistory(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        return ResponseEntity
                .ok(ApiResponse.ok(coachMessageRepository.findTop20ByCoupleIdOrderByCreatedAtAsc(coupleId)));
    }

    @GetMapping("/insights/latest")
    public ResponseEntity<ApiResponse<AiInsight>> getLatestInsight(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        return insightRepository.findTopByCoupleIdOrderByWeekStartDesc(coupleId)
                .map(insight -> ResponseEntity.ok(ApiResponse.ok(insight)))
                .orElse(ResponseEntity.ok(ApiResponse.error("No insights yet. Chat more to generate insights!")));
    }

    @GetMapping("/personas")
    public ResponseEntity<ApiResponse<java.util.List<PartnerPersona>>> getPersonas(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(personaRepository.findByCoupleId(coupleId)));
    }
}
