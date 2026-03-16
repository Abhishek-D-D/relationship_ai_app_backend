package com.couplespace.controller;

import com.couplespace.dto.ApiResponse;
import com.couplespace.entity.MoodSnapshot;
import com.couplespace.entity.User;
import com.couplespace.repository.MoodSnapshotRepository;
import com.couplespace.repository.UserRepository;
import com.couplespace.service.CoupleService;
import com.couplespace.service.MoodAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mood")
@RequiredArgsConstructor
public class MoodController {

    private final MoodSnapshotRepository moodSnapshotRepository;
    private final MoodAnalysisService moodAnalysisService;
    private final CoupleService coupleService;
    private final UserRepository userRepository;

    /**
     * GET /api/v1/mood/partner
     * Returns the latest mood snapshot for the current user's partner.
     */
    @GetMapping("/partner")
    public ResponseEntity<ApiResponse<MoodSnapshot>> getPartnerMood(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        var couple = coupleService.getCoupleById(coupleId);

        // Identify partner
        User partner = couple.getPartner1().getUserId().equals(user.getUserId())
                ? couple.getPartner2()
                : couple.getPartner1();

        if (partner == null) {
            return ResponseEntity.ok(ApiResponse.error("Partner has not joined yet"));
        }

        return moodSnapshotRepository
                .findTopByCoupleIdAndUserIdOrderByCreatedAtDesc(coupleId, partner.getUserId())
                .map(snapshot -> ResponseEntity.ok(ApiResponse.ok(snapshot)))
                .orElse(ResponseEntity.ok(ApiResponse.error("No mood data yet. Chat more to generate insights!")));
    }

    /**
     * GET /api/v1/mood/me
     * Returns this user's own latest mood snapshot.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MoodSnapshot>> getMyMood(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        return moodSnapshotRepository
                .findTopByCoupleIdAndUserIdOrderByCreatedAtDesc(coupleId, user.getUserId())
                .map(snapshot -> ResponseEntity.ok(ApiResponse.ok(snapshot)))
                .orElse(ResponseEntity.ok(ApiResponse.error("No mood data yet")));
    }

    /**
     * POST /api/v1/mood/analyse
     * Manually trigger mood analysis for the current user (refreshes immediately).
     */
    @PostMapping("/analyse")
    public ResponseEntity<ApiResponse<MoodSnapshot>> analyseMood(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        MoodSnapshot result = moodAnalysisService.analyseMoodForUser(coupleId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * PUT /api/v1/mood/fcm-token
     * Updates user's FCM push notification token.
     */
    @PutMapping("/fcm-token")
    public ResponseEntity<ApiResponse<String>> updateFcmToken(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String token = body.get("fcmToken");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("fcmToken is required"));
        }
        User dbUser = userRepository.findById(user.getUserId()).orElseThrow();
        dbUser.setFcmToken(token);
        userRepository.save(dbUser);
        return ResponseEntity.ok(ApiResponse.ok("FCM token updated 💜"));
    }
}
