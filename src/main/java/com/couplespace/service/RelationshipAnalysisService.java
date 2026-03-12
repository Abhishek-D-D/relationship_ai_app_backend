package com.couplespace.service;

import com.couplespace.entity.AiInsight;
import com.couplespace.entity.RelationshipMetrics;
import com.couplespace.repository.AiInsightRepository;
import com.couplespace.repository.CallLogRepository;
import com.couplespace.repository.MessageRepository;
import com.couplespace.repository.PartnerPersonaRepository;
import com.couplespace.repository.RelationshipMetricsRepository;
import com.couplespace.repository.UserRepository;
import com.couplespace.entity.User;
import com.couplespace.entity.PartnerPersona;
import com.couplespace.entity.CallLog;
import com.couplespace.service.CoupleService;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelationshipAnalysisService {

    private final MessageRepository messageRepository;
    private final RelationshipMetricsRepository metricsRepository;
    private final AiInsightRepository insightRepository;
    private final OpenAiService openAiService;
    private final CallLogRepository callLogRepository;
    private final PartnerPersonaRepository personaRepository;
    private final UserRepository userRepository;
    private final CoupleService coupleService;

    private static final Set<String> POSITIVE_KEYWORDS = Set.of(
            "love", "miss", "happy", "amazing", "beautiful", "thank",
            "grateful", "proud", "exciting", "together", "kiss", "heart",
            "wonderful", "sweet", "cute", "care", "adore", "treasure");

    private static final Set<String> CONFLICT_KEYWORDS = Set.of(
            "angry", "hate", "upset", "frustrated", "fight", "stop",
            "leave", "ignore", "hurt", "disappointed", "annoyed", "tired",
            "stupid", "awful", "worst", "never", "always blame");

    @Async
    @Transactional
    public void analyzeAndGenerateInsight(UUID coupleId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);

        var messages = messageRepository.findByCoupleIdAndCreatedAtBetween(
                coupleId,
                weekStart.atStartOfDay(),
                today.atTime(23, 59, 59));

        var calls = callLogRepository.findByCoupleIdAndStartedAtBetween(
                coupleId,
                weekStart.atStartOfDay(),
                today.atTime(23, 59, 59));

        int msgCount = messages.size();
        if (msgCount == 0) {
            log.info("No messages for couple {} — skipping analysis", coupleId);
            return;
        }

        // Compute scores
        double avgRT = computeAvgResponseTime(messages);
        int positiveScore = computeSentimentScore(messages, POSITIVE_KEYWORDS);
        int conflictScore = computeSentimentScore(messages, CONFLICT_KEYWORDS);

        int callCount = calls.size();
        int totalCallMins = calls.stream().mapToInt(CallLog::getDurationSeconds).sum() / 60;

        int healthScore = computeHealthScore(msgCount, avgRT, positiveScore, conflictScore, callCount, totalCallMins);

        // Save metrics
        RelationshipMetrics metrics = RelationshipMetrics.builder()
                .coupleId(coupleId)
                .periodStart(weekStart)
                .periodEnd(today)
                .messageCount(msgCount)
                .avgResponseTimeMins(BigDecimal.valueOf(avgRT))
                .positiveScore(positiveScore)
                .conflictScore(conflictScore)
                .callCount(callCount)
                .totalCallMinutes(totalCallMins)
                .build();
        metricsRepository.save(metrics);

        // Update Partner Personas for both partners
        generatePartnerPersonas(coupleId, messages);

        // Generate AI insight via OpenAI
        try {
            String prompt = buildInsightPrompt(msgCount, avgRT, positiveScore, conflictScore, healthScore, callCount,
                    totalCallMins);
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

    public int computeHealthScore(int msgCount, double avgRTMins, int positiveScore, int conflictScore, int callCount,
            int callMins) {
        double freqScore = Math.min(msgCount / 2.0, 30); // max 30
        double responseScore = Math.max(0, 25 - avgRTMins); // max 25
        double posPoints = positiveScore * 0.30; // max 30
        double conflictPenalty = conflictScore * 0.15; // max -15

        // Add call bonus: 1 point per call (max 5) + 1 point per 10 mins (max 5)
        double callBonus = Math.min(callCount, 5) + Math.min(callMins / 10.0, 5);

        return (int) Math.max(0,
                Math.min(100, freqScore + responseScore + posPoints - conflictPenalty + callBonus + 10));
    }

    public int calculateVibeMatch(UUID coupleId) {
        // Logic to compare onboarding responses and return a match score
        // For now, return a placeholder that we'll calculate based on actual data
        return 85;
    }

    private double computeAvgResponseTime(List<com.couplespace.entity.Message> messages) {
        if (messages.size() < 2)
            return 0;
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

    private String buildInsightPrompt(int msgCount, double avgRT, int positive, int conflict, int health, int callCount,
            int callMins) {
        return """
                You are an expert relationship psychologist. Analyze these weekly metrics:
                - Messages exchanged: %d
                - Average response time: %.1f minutes
                - Positivity score: %d/100
                - Conflict indicator: %d/100
                - Calls: %d total, %d minutes combined
                - Overall health score: %d/100

                Provide a warm, addictive, and deeply personal analysis in exactly two sections:
                SUMMARY: (2-3 sentences about the relationship state this week. Be specific and encouraging.)
                SUGGESTIONS:
                1. (actionable tip for deeper connection)
                2. (tip based on communication style)
                3. (a "challenge" for the couple this week)
                """.formatted(msgCount, avgRT, positive, conflict, callCount, callMins, health);
    }

    private void generatePartnerPersonas(UUID coupleId, List<com.couplespace.entity.Message> messages) {
        var couple = coupleService.getCoupleById(coupleId);
        var partners = List.of(couple.getPartner1(), couple.getPartner2());

        for (User partner : partners) {
            if (partner == null)
                continue;

            var partnerMsgs = messages.stream()
                    .filter(m -> m.getSenderId().equals(partner.getUserId()))
                    .map(m -> m.getContent())
                    .filter(Objects::nonNull)
                    .limit(50) // Analyze last 50 messages
                    .toList();

            if (partnerMsgs.isEmpty())
                continue;

            try {
                String prompt = """
                        Analyze these recent messages from user '%s' in a relationship app:
                        %s

                        Return a JSON object with:
                        - communicationStyle: (a creative title like 'The Deep Thinker', 'The Playful Tease', etc.)
                        - loveLanguage: (top hypothesized love language)
                        - aura: (a short vibe description like 'Gentle and Supporting' or 'Radiant and Passionate')
                        - traitList: (comma-separated list of 3 personality descriptors)
                        """.formatted(partner.getName(), String.join("\n", partnerMsgs));

                String response = openAiService.chatCompletion(prompt);

                // Simple parsing (could be more robust with Jackson)
                PartnerPersona persona = personaRepository.findByUserId(partner.getUserId())
                        .orElse(PartnerPersona.builder()
                                .userId(partner.getUserId())
                                .coupleId(coupleId)
                                .build());

                persona.setCommunicationStyle(parseValue(response, "communicationStyle"));
                persona.setPrimaryLoveLanguage(parseValue(response, "loveLanguage"));
                persona.setAura(parseValue(response, "aura"));
                persona.setTraits(parseValue(response, "traitList"));

                personaRepository.save(persona);
            } catch (Exception e) {
                log.error("Failed to generate persona for {}: {}", partner.getName(), e.getMessage());
            }
        }
    }

    private String parseValue(String json, String key) {
        try {
            int keyIdx = json.indexOf("\"" + key + "\":");
            if (keyIdx < 0)
                return "N/A";
            int startIdx = json.indexOf("\"", keyIdx + key.length() + 3) + 1;
            int endIdx = json.indexOf("\"", startIdx);
            return json.substring(startIdx, endIdx);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private String extractSummary(String aiResponse) {
        if (aiResponse == null)
            return "";
        int summaryIdx = aiResponse.indexOf("SUMMARY:");
        int suggIdx = aiResponse.indexOf("SUGGESTIONS:");
        if (summaryIdx >= 0 && suggIdx > summaryIdx) {
            return aiResponse.substring(summaryIdx + 8, suggIdx).trim();
        }
        return aiResponse.length() > 300 ? aiResponse.substring(0, 300) : aiResponse;
    }

    private String extractSuggestions(String aiResponse) {
        if (aiResponse == null)
            return "";
        int suggIdx = aiResponse.indexOf("SUGGESTIONS:");
        if (suggIdx >= 0) {
            return aiResponse.substring(suggIdx + 12).trim();
        }
        return "";
    }
}
