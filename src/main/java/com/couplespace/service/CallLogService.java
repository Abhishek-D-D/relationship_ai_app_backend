package com.couplespace.service;

import com.couplespace.entity.CallLog;
import com.couplespace.repository.CallLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CallLogService {

    private final CallLogRepository callLogRepository;

    public CallLog logCall(UUID coupleId, UUID initiatorId, CallLog.CallType type, int durationSeconds,
            CallLog.CallStatus status) {
        CallLog callLog = CallLog.builder()
                .coupleId(coupleId)
                .initiatorId(initiatorId)
                .callType(type)
                .durationSeconds(durationSeconds)
                .status(status)
                .endedAt(LocalDateTime.now())
                .build();
        return callLogRepository.save(callLog);
    }
}
