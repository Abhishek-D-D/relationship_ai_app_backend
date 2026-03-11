package com.couplespace.controller;

import com.couplespace.dto.ApiResponse;
import com.couplespace.dto.CoupleDto;
import com.couplespace.entity.User;
import com.couplespace.service.CoupleService;
import com.couplespace.service.RelationshipAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/couple")
@RequiredArgsConstructor
public class CoupleController {

    private final CoupleService coupleService;
    private final RelationshipAnalysisService analysisService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<CoupleDto>> createCouple(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(coupleService.createCouple(user)));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<CoupleDto>> joinCouple(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal User user) {
        String inviteCode = body.get("inviteCode");
        if (inviteCode == null || inviteCode.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("inviteCode is required"));
        }
        return ResponseEntity.ok(ApiResponse.ok(coupleService.joinCouple(inviteCode, user)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CoupleDto>> getMyCoupleInfo(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ApiResponse.ok(coupleService.getCoupleForUser(user.getUserId())));
    }

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<String>> triggerAnalysis(
            @AuthenticationPrincipal User user) {
        var coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        analysisService.analyzeAndGenerateInsight(coupleId);
        return ResponseEntity.ok(ApiResponse.ok("Analysis triggered", "Processing in background"));
    }
}
