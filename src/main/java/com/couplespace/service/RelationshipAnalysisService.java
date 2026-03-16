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
import java.util.stream.Collectors;
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
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

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

        // 1. Core Metrics
        double avgRT = computeAvgResponseTime(messages);
        int callCount = calls.size();
        int totalCallMins = calls.stream().mapToInt(CallLog::getDurationSeconds).sum() / 60;

        // 2. Advanced Sentiment Analysis via LLM
        Map<String, Integer> sentiment = analyzeSentimentWithAi(messages);
        int positiveScore = sentiment.getOrDefault("positivity", 50);
        int conflictScore = sentiment.getOrDefault("conflict", 20);

        int healthScore = computeHealthScore(msgCount, avgRT, positiveScore, conflictScore, callCount, totalCallMins);

        // 3. Save metrics
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

        // 4. Update Partner Personas
        generatePartnerPersonas(coupleId, messages);

        // 5. Generate AI insight
        try {
            String prompt = buildInsightPrompt(msgCount, avgRT, positiveScore, conflictScore, healthScore, callCount,
                    totalCallMins);
            String aiResponse = openAiService.chatCompletion(prompt);

            // Expected format: {"summary": "...", "suggestions": ["...", "..."]}
            String cleanedJson = extractJson(aiResponse);
            var responseNode = objectMapper.readTree(cleanedJson);

            AiInsight insight = AiInsight.builder()
                    .coupleId(coupleId)
                    .weekStart(weekStart)
                    .healthScore(healthScore)
                    .summary(responseNode.has("summary") ? responseNode.get("summary").asText() : "Analysis pending")
                    .suggestions(responseNode.has("suggestions") ? responseNode.get("suggestions").toString() : "[]")
                    .build();
            insightRepository.save(insight);
            log.info("Insight generated for couple {} | health={}", coupleId, healthScore);
        } catch (Exception e) {
            log.error("Failed to generate AI insight for couple {}: {}", coupleId, e.getMessage());
            // Safe fallback
            saveFallbackInsight(coupleId, weekStart, healthScore);
        }
    }

    private Map<String, Integer> analyzeSentimentWithAi(List<com.couplespace.entity.Message> messages) {
        String recentTraffic = messages.stream()
                .map(m -> m.getSenderId() + ": " + m.getContent())
                .limit(40)
                .collect(Collectors.joining("\n"));

        String prompt = """
                Analyze the following relationship chat log.
                Rate the overall positivity and conflict intensity.

                Chat Log:
                %s

                Return ONLY a JSON object: {"positivity": 0-100, "conflict": 0-100}
                """.formatted(recentTraffic);

        try {
            String response = openAiService.chatCompletion(prompt);
            String cleanedJson = extractJson(response);
            var node = objectMapper.readTree(cleanedJson);
            Map<String, Integer> result = new HashMap<>();
            result.put("positivity", node.has("positivity") ? node.get("positivity").asInt() : 70);
            result.put("conflict", node.has("conflict") ? node.get("conflict").asInt() : 10);
            return result;
        } catch (Exception e) {
            log.error("LLM Sentiment analysis failed: {}", e.getMessage());
            return Map.of("positivity", 70, "conflict", 10); // Default neutral/positive
        }
    }

    public int computeHealthScore(int msgCount, double avgRTMins, int positiveScore, int conflictScore, int callCount,
            int callMins) {
        double freqScore = Math.min(msgCount / 2.0, 30);
        double responseScore = Math.max(0, 25 - avgRTMins);
        double posPoints = positiveScore * 0.30;
        double conflictPenalty = conflictScore * 0.20;

        double callBonus = Math.min(callCount, 5) + Math.min(callMins / 10.0, 5);

        return (int) Math.max(0,
                Math.min(100, freqScore + responseScore + posPoints - conflictPenalty + callBonus + 10));
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
                if (mins >= 0 && mins < 1440) {
                    total += mins;
                    count++;
                }
            }
        }
        return count > 0 ? total / count : 0;
    }

    private String buildInsightPrompt(int msgCount, double avgRT, int positive, int conflict, int health, int callCount,
            int callMins) {
        return """
                You are a premium relationship analyst. Deeply analyze these metrics:
                - Messages: %d, Response Time: %.1f min
                - Positivity: %d, Conflict: %d
                - Calls: %d (%d mins)
                - Score: %d

                Generate a summary and exactly 3 suggestions.
                Return ONLY valid JSON:
                {
                  "summary": "cinematic 2-3 sentence analysis",
                  "suggestions": ["specific tip 1", "specific tip 2", "specific tip 3"]
                }
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
                    .map(com.couplespace.entity.Message::getContent)
                    .filter(Objects::nonNull)
                    .limit(50)
                    .collect(Collectors.toList());

            if (partnerMsgs.isEmpty())
                continue;

            try {
                String prompt = """
                        Analyze messages from '%s':
                        %s

                        Return ONLY valid JSON:
                        {
                          "style": "The Playful Romantic",
                          "loveLanguage": "Words of Affirmation",
                          "aura": "Gentle and Passionate",
                          "traits": "kind, expressive, loyal"
                        }
                        """.formatted(partner.getName(), String.join("\n", partnerMsgs));

                String response = openAiService.chatCompletion(prompt);
                String cleanedJson = extractJson(response);
                var node = objectMapper.readTree(cleanedJson);

                var persona = personaRepository.findByUserId(partner.getUserId())
                        .orElse(PartnerPersona.builder().userId(partner.getUserId()).coupleId(coupleId).build());

                persona.setCommunicationStyle(node.has("style") ? node.get("style").asText() : "Balanced");
                persona.setPrimaryLoveLanguage(
                        node.has("loveLanguage") ? node.get("loveLanguage").asText() : "Affirmation");
                persona.setAura(node.has("aura") ? node.get("aura").asText() : "Warm");
                persona.setTraits(node.has("traits") ? node.get("traits").asText() : "kind, expressive");

                personaRepository.save(persona);
            } catch (Exception e) {
                log.error("Persona error: {}", e.getMessage());
            }
        }
    }

    private void saveFallbackInsight(UUID coupleId, LocalDate weekStart, int healthScore) {
        AiInsight insight = AiInsight.builder()
                .coupleId(coupleId)
                .weekStart(weekStart)
                .healthScore(healthScore)
                .summary("We're still gathering insights from your beautiful journey this week.")
                .suggestions(
                        "[\"Keep the conversation flowing\", \"Share a small wins today\", \"Tell them one thing you appreciate\"]")
                .build();
        insightRepository.save(insight);
    }

    private String extractJson(String text) {
        if (text == null)
            return "{}";
        int start = text.indexOf("{");
        int end = text.lastIndexOf("}");
        if (start != -1 && end != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return "{}";
    }
}
