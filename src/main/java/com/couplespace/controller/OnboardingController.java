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
import java.util.UUID;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final CoupleService coupleService;

    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<OnboardingQuestion>>> getQuestions() {
        return ResponseEntity.ok(ApiResponse.ok(onboardingService.getAllQuestions()));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<String>> submit(
            @AuthenticationPrincipal User user,
            @RequestBody List<OnboardingResponse> responses) {

        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        onboardingService.submitResponses(user.getUserId(), coupleId, responses);

        return ResponseEntity.ok(ApiResponse.ok("Responses submitted and processed! 💜"));
    }
}
