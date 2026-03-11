package com.couplespace.service;

import com.couplespace.entity.AiInsight;
import com.couplespace.entity.RelationshipMetrics;
import com.couplespace.repository.AiInsightRepository;
import com.couplespace.repository.MessageRepository;
import com.couplespace.repository.RelationshipMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipAnalysisService {

    private final MessageRepository messageRepository;
    private final RelationshipMetricsRepository metricsRepository;
    private final AiInsightRepository insightRepository;
    private final OpenAiService openAiService;

    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
        "love", "miss", "happy", "amazing", "beautiful", "thank",
        "grateful", "proud", "exciting", "together", "kiss", "heart",
        "wonderful", "sweet", "cute", "care", "adore", "treasure"
    );

    private static final Set<String> CONFLICT_KEYWORDS = Set.of(
        "angry", "hate", "upset", "frustrated", "fight", "stop",
        "leave", "ignore", "hurt", "disappointed", "annoyed", "tired",
        "stupid", "awful", "worst", "never", "always blame"
    );

    @Async
    @Transactional
    public void analyzeAndGenerateInsight(UUID coupleId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);

        var messages = messageRepository.findByCoupleIdAndCreatedAtBetween(
            coupleId,
            weekStart.atStartOfDay(),
            today.atTime(23, 59, 59)
        );

        int msgCount = messages.size();
        if (msgCount == 0) {
            log.info("No messages for couple {} — skipping analysis", coupleId);
            return;
        }

        // Compute scores
        double avgRT = computeAvgResponseTime(messages);
        int positiveScore = computeSentimentScore(messages, POSITIVE_KEYWORDS);
        int conflictScore = computeSentimentScore(messages, CONFLICT_KEYWORDS);
        int healthScore   = computeHealthScore(msgCount, avgRT, positiveScore, conflictScore);

        // Save metrics
        RelationshipMetrics metrics = RelationshipMetrics.builder()
                .coupleId(coupleId)
                .periodStart(weekStart)
                .periodEnd(today)
                .messageCount(msgCount)
                .avgResponseTimeMins(BigDecimal.valueOf(avgRT))
                .positiveScore(positiveScore)
                .conflictScore(conflictScore)
                .build();
        metricsRepository.save(metrics);

        // Generate AI insight via OpenAI
        try {
            String prompt = buildInsightPrompt(msgCount, avgRT, positiveScore, conflictScore, healthScore);
            String aiResponse = openAiService.chatCompletion(prompt);

            AiInsight insight = AiInsight.builder()
                    .coupleId(coupleId)
                    .weekStart(weekStart)
                    .healthScore(healthScore)
                    .summary(extractSummary(aiResponse))
                    .suggestions(extractSuggestions(aiResponse))
                    .build();
            insightRepository.save(insight);
            log.info("Insight generated for couple {} | health={}", coupleId, healthScore);
        } catch (Exception e) {
            log.error("Failed to generate AI insight for couple {}: {}", coupleId, e.getMessage());
            // Save insight without AI summary
            AiInsight insight = AiInsight.builder()
                    .coupleId(coupleId)
                    .weekStart(weekStart)
                    .healthScore(healthScore)
                    .summary("Analysis complete for this week.")
                    .suggestions("Keep communicating regularly and express appreciation daily.")
                    .build();
            insightRepository.save(insight);
        }
    }

    public int computeHealthScore(int msgCount, double avgRTMins, int positiveScore, int conflictScore) {
        double freqScore     = Math.min(msgCount / 2.0, 30);        // max 30
        double responseScore = Math.max(0, 25 - avgRTMins);         // max 25
        double posPoints     = positiveScore * 0.30;                 // max 30
        double conflictPenalty = conflictScore * 0.15;              // max -15
        return (int) Math.max(0, Math.min(100, freqScore + responseScore + posPoints - conflictPenalty + 10));
    }

    private double computeAvgResponseTime(List<com.couplespace.entity.Message> messages) {
        if (messages.size() < 2) return 0;
        double total = 0;
        int count = 0;
        for (int i = 1; i < messages.size(); i++) {
            var prev = messages.get(i - 1);
            var curr = messages.get(i);
            if (!prev.getSenderId().equals(curr.getSenderId())) {
                long mins = Duration.between(prev.getCreatedAt(), curr.getCreatedAt()).toMinutes();
                if (mins >= 0 && mins < 1440) { // ignore gaps > 1 day
                    total += mins;
                    count++;
                }
            }
        }
        return count > 0 ? total / count : 0;
    }

    private int computeSentimentScore(List<com.couplespace.entity.Message> messages, Set<String> keywords) {
        long matched = messages.stream()
                .filter(m -> m.getContent() != null)
                .filter(m -> keywords.stream().anyMatch(
                        kw -> m.getContent().toLowerCase().contains(kw)))
                .count();
        return (int) Math.min(100, (matched * 100) / Math.max(1, messages.size()));
    }

    private String buildInsightPrompt(int msgCount, double avgRT, int positive, int conflict, int health) {
        return """
            You are an expert relationship psychologist. Analyze these weekly communication metrics:
            - Messages exchanged: %d
            - Average response time: %.1f minutes
            - Positivity score: %d/100
            - Conflict indicator: %d/100
            - Computed health score: %d/100
            
            Provide a warm, empathetic analysis in exactly two sections:
            SUMMARY: (2-3 sentences about the relationship state this week)
            SUGGESTIONS:
            1. (first actionable tip)
            2. (second actionable tip)
            3. (third actionable tip)
            """.formatted(msgCount, avgRT, positive, conflict, health);
    }

    private String extractSummary(String aiResponse) {
        if (aiResponse == null) return "";
        int summaryIdx = aiResponse.indexOf("SUMMARY:");
        int suggIdx = aiResponse.indexOf("SUGGESTIONS:");
        if (summaryIdx >= 0 && suggIdx > summaryIdx) {
            return aiResponse.substring(summaryIdx + 8, suggIdx).trim();
        }
        return aiResponse.length() > 300 ? aiResponse.substring(0, 300) : aiResponse;
    }

    private String extractSuggestions(String aiResponse) {
        if (aiResponse == null) return "";
        int suggIdx = aiResponse.indexOf("SUGGESTIONS:");
        if (suggIdx >= 0) {
            return aiResponse.substring(suggIdx + 12).trim();
        }
        return "";
    }
}
