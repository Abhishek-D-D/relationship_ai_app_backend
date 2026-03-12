package com.couplespace.service;

import com.couplespace.entity.AiInsight;
import com.couplespace.entity.RelationshipMetrics;
import com.couplespace.entity.PartnerPersona;
import com.couplespace.repository.AiInsightRepository;
import com.couplespace.repository.RelationshipMetricsRepository;
import com.couplespace.repository.PartnerPersonaRepository;
import com.couplespace.repository.AiCoachMessageRepository;
import com.couplespace.entity.AiCoachMessage;
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
    private final PartnerPersonaRepository personaRepository;
    private final AiCoachMessageRepository coachMessageRepository;

    public String chat(UUID coupleId, UUID userId, String userMessage) {
        // Save user message
        coachMessageRepository.save(AiCoachMessage.builder()
                .coupleId(coupleId)
                .senderId(userId)
                .role("USER")
                .content(userMessage)
                .build());

        // Enrich prompt with relationship context
        StringBuilder contextBuilder = new StringBuilder(SYSTEM_PROMPT);
        contextBuilder.append("\n\n=== Current Relationship Context ===\n");

        insightRepository.findTopByCoupleIdOrderByWeekStartDesc(coupleId).ifPresent(insight -> {
            contextBuilder.append("Latest health score: ").append(insight.getHealthScore()).append("/100\n");
            contextBuilder.append("Recent summary: ").append(insight.getSummary()).append("\n");
        });

        metricsRepository.findTopByCoupleIdOrderByPeriodStartDesc(coupleId).ifPresent(metrics -> {
            contextBuilder.append("Recent messages: ").append(metrics.getMessageCount()).append("/week\n");
            contextBuilder.append("Recent calls: ").append(metrics.getCallCount()).append(" calls, ")
                    .append(metrics.getTotalCallMinutes()).append(" mins\n");
            contextBuilder.append("Avg response time: ").append(metrics.getAvgResponseTimeMins()).append(" min\n");
            contextBuilder.append("Positivity score: ").append(metrics.getPositiveScore()).append("/100\n");
        });

        personaRepository.findByCoupleId(coupleId).forEach(persona -> {
            contextBuilder.append("\n=== Partner Persona (").append(persona.getUserId()).append(") ===\n");
            contextBuilder.append("Style: ").append(persona.getCommunicationStyle()).append("\n");
            contextBuilder.append("Primary Love Language: ").append(persona.getPrimaryLoveLanguage()).append("\n");
            contextBuilder.append("Traits: ").append(persona.getTraits()).append("\n");
            contextBuilder.append("Aura: ").append(persona.getAura()).append("\n");
        });

        // Add history
        contextBuilder.append("\n\n=== Conversation History with Aria ===\n");
        coachMessageRepository.findTop20ByCoupleIdOrderByCreatedAtAsc(coupleId).forEach(msg -> {
            contextBuilder.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        });

        try {
            String reply = openAiService.chatCompletion(contextBuilder.toString(), userMessage);

            // Save Assistant response
            coachMessageRepository.save(AiCoachMessage.builder()
                    .coupleId(coupleId)
                    .role("ASSISTANT")
                    .content(reply)
                    .build());

            return reply;
        } catch (Exception e) {
            log.error("AI coach error: {}", e.getMessage());
            return "I'm having trouble connecting right now. Please try again in a moment. 💜 " +
                    "Remember: open communication is always the first step to deeper understanding.";
        }
    }
}
