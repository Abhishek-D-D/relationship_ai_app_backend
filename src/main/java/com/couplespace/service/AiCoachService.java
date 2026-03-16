package com.couplespace.service;

import com.couplespace.entity.AiInsight;
import com.couplespace.entity.RelationshipMetrics;
import com.couplespace.entity.PartnerPersona;
import com.couplespace.repository.AiInsightRepository;
import com.couplespace.repository.RelationshipMetricsRepository;
import com.couplespace.repository.PartnerPersonaRepository;
import com.couplespace.repository.AiCoachMessageRepository;
import com.couplespace.repository.OnboardingResponseRepository;
import com.couplespace.repository.OnboardingQuestionRepository;
import com.couplespace.entity.AiCoachMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCoachService {

    private final OpenAiService openAiService;
    private final AiCoachMessageRepository coachMessageRepository;
    private final RelationshipContextService contextService;

    private static final String SYSTEM_PROMPT = """
            You are Aria, a world-class AI relationship coach for CoupleSpace AI.
            You aren't just an assistant; you are a trusted confidant who deeply understands the couple's unique bond.

            YOUR CORE PHILOSOPHY:
            - **Radical Empathy**: Always validate emotions first.
            - **Data-Driven Insight**: Weave in their actual relationship metrics (health score, mood, communication style) naturally.
            - **Neutral Ground**: Never take sides. Support the *relationship* as the third entity.
            - **Gottman & Attachment Expertise**: Use principles from Attachment Theory and the Gottman Method invisibly in your advice.

            YOUR PERSONALITY:
            - Sophisticated, warm, and slightly poetic.
            - Uses the partners' names or communication styles to tailor advice.
            - Ends every message with a "Micro-Connection Challenge" — one tiny, specific act they can do right now.

            CONSTRAINTS:
            - Keep responses concise (3-4 sentences max).
            - Avoid generic "I am an AI" language.
            - If they are in a high-stress state (based on mood data), be extra gentle and grounding.
            """;

    public String chat(UUID coupleId, UUID userId, String userMessage) {
        // 1. Save user message
        coachMessageRepository.save(AiCoachMessage.builder()
                .coupleId(coupleId)
                .senderId(userId)
                .role("USER")
                .content(userMessage)
                .build());

        // 2. Gather unified relationship intelligence
        String relationshipIntelligence = contextService.getUnifiedContext(coupleId);

        // 3. Build the sophisticated prompt
        StringBuilder finalPrompt = new StringBuilder(SYSTEM_PROMPT);
        finalPrompt.append("\n\n=== RELATIONSHIP INTELLIGENCE (FOR YOUR EYES ONLY) ===\n");
        finalPrompt.append(relationshipIntelligence);

        finalPrompt.append("\n\n=== RECENT CONVERSATION HISTORY WITH ARIA ===\n");
        coachMessageRepository.findTop20ByCoupleIdOrderByCreatedAtAsc(coupleId).forEach(msg -> {
            finalPrompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        });

        try {
            // 4. Generate AI response
            String reply = openAiService.chatCompletion(finalPrompt.toString(), userMessage);

            // 5. Save and return
            coachMessageRepository.save(AiCoachMessage.builder()
                    .coupleId(coupleId)
                    .role("ASSISTANT")
                    .content(reply)
                    .build());

            return reply;
        } catch (Exception e) {
            log.error("Aria system error for couple {}: {}", coupleId, e.getMessage());
            return "I'm momentarily offline, but I'm still here for you both. Take a deep breath together. I'll be back in just a second. 💜";
        }
    }
}
