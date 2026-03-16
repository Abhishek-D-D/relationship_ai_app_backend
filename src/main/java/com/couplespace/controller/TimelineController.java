package com.couplespace.controller;

import com.couplespace.dto.ApiResponse;
import com.couplespace.entity.RelationshipMilestone;
import com.couplespace.entity.User;
import com.couplespace.service.CoupleService;
import com.couplespace.service.TimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;
    private final CoupleService coupleService;

    /**
     * GET /api/v1/timeline
     * Returns sorted milestone list for the authenticated couple.
     * Both partners share the same view.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<RelationshipMilestone>>> getTimeline(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        List<RelationshipMilestone> milestones = timelineService.getTimeline(coupleId);
        return ResponseEntity.ok(ApiResponse.ok(milestones));
    }

    /**
     * POST /api/v1/timeline/extract
     * Triggers async LLM extraction of milestones from chat history.
     * Returns 202 Accepted immediately; extraction runs in background.
     */
    @PostMapping("/extract")
    public ResponseEntity<ApiResponse<String>> extractTimeline(
            @AuthenticationPrincipal User user) {
        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());
        timelineService.extractMilestones(coupleId);
        return ResponseEntity.accepted()
                .body(ApiResponse.ok("Timeline extraction started! Refresh in a few seconds ✨"));
    }
}
