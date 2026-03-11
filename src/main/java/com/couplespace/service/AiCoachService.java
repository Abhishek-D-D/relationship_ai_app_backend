package com.couplespace.service;

import com.couplespace.entity.AiInsight;
import com.couplespace.entity.RelationshipMetrics;
import com.couplespace.repository.AiInsightRepository;
import com.couplespace.repository.RelationshipMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCoachService {

    private static final String SYSTEM_PROMPT = """
        You are Aria, a warm and empathetic AI relationship coach for CoupleSpace AI.
        You help couples communicate better, understand each other, and build a stronger bond.
        You have deep knowledge of the Gottman Method, Love Languages, and Attachment Theory.
        
        Your personality:
        - Warm, caring, and specific — never vague
        - Uses emojis sparingly but meaningfully (💜, 🌹, ✨)
        - Never takes sides; always supports mutual understanding
        - References the couple's actual metrics when relevant
        - Keeps responses under 4 sentences unless asked for more
        - Always ends with ONE specific, immediately actionable step
        
        Do not reveal that you are powered by any specific AI model.
        If any question involves mental health crisis, gently suggest professional help.
        """;

    private final OpenAiService openAiService;
    private final AiInsightRepository insightRepository;
    private final RelationshipMetricsRepository metricsRepository;

    public String chat(UUID coupleId, String userMessage) {
        // Enrich prompt with relationship context
        StringBuilder contextBuilder = new StringBuilder(SYSTEM_PROMPT);
        contextBuilder.append("\n\n=== Current Relationship Context ===\n");

        insightRepository.findTopByCoupleIdOrderByWeekStartDesc(coupleId).ifPresent(insight -> {
            contextBuilder.append("Latest health score: ").append(insight.getHealthScore()).append("/100\n");
            contextBuilder.append("Recent summary: ").append(insight.getSummary()).append("\n");
        });

        metricsRepository.findTopByCoupleIdOrderByPeriodStartDesc(coupleId).ifPresent(metrics -> {
            contextBuilder.append("Recent messages: ").append(metrics.getMessageCount()).append("/week\n");
            contextBuilder.append("Avg response time: ").append(metrics.getAvgResponseTimeMins()).append(" min\n");
            contextBuilder.append("Positivity score: ").append(metrics.getPositiveScore()).append("/100\n");
        });

        try {
            return openAiService.chatCompletion(contextBuilder.toString(), userMessage);
        } catch (Exception e) {
            log.error("AI coach error: {}", e.getMessage());
            return "I'm having trouble connecting right now. Please try again in a moment. 💜 " +
                   "Remember: open communication is always the first step to deeper understanding.";
        }
    }
}
