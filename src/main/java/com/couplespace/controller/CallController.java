package com.couplespace.controller;

import com.couplespace.dto.ApiResponse;
import com.couplespace.entity.CallLog;
import com.couplespace.entity.User;
import com.couplespace.service.CallLogService;
import com.couplespace.service.CoupleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/calls")
@RequiredArgsConstructor
public class CallController {

    private final CallLogService callLogService;
    private final CoupleService coupleService;

    @PostMapping("/log")
    public ResponseEntity<ApiResponse<CallLog>> logCall(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        UUID coupleId = coupleService.getCoupleIdForUser(user.getUserId());

        CallLog.CallType type = CallLog.CallType.valueOf((String) body.getOrDefault("type", "VOICE"));
        int duration = (int) body.getOrDefault("durationSeconds", 0);
        CallLog.CallStatus status = CallLog.CallStatus.valueOf((String) body.getOrDefault("status", "COMPLETED"));

        CallLog saved = callLogService.logCall(coupleId, user.getUserId(), type, duration, status);
        return ResponseEntity.ok(ApiResponse.ok(saved));
    }
}
