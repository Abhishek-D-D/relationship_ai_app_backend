package com.couplespace.controller;

import com.couplespace.dto.ApiResponse;
import com.couplespace.entity.OnboardingQuestion;
import com.couplespace.entity.OnboardingResponse;
import com.couplespace.entity.User;
import com.couplespace.service.CoupleService;
import com.couplespace.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final CoupleService coupleService;

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getStatus(@AuthenticationPrincipal User user) {
        boolean completed = onboardingService.isOnboardingComplete(user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("completed", completed)));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<String>> submit(
            @AuthenticationPrincipal User user,
            @RequestBody List<OnboardingResponse> responses) {

        UUID coupleId = null;
        try {
            coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        } catch (Exception e) {
            // User not in a couple yet, that's fine
        }

        onboardingService.submitResponses(user.getUserId(), coupleId, responses);

        return ResponseEntity.ok(ApiResponse.ok("Responses submitted and processed! 💜"));
    }
}
