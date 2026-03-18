package com.couplespace.service;

import com.couplespace.entity.AiInsight;
import com.couplespace.entity.RelationshipMetrics;
import com.couplespace.entity.PartnerPersona;
import com.couplespace.repository.AiInsightRepository;
import com.couplespace.repository.RelationshipMetricsRepository;
import com.couplespace.repository.PartnerPersonaRepository;
import com.couplespace.repository.AiCoachMessageRepository;
import com.couplespace.repository.MessageRepository;
import com.couplespace.entity.AiCoachMessage;
import com.couplespace.entity.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCoachService {

    private final OpenAiService openAiService;
    private final AiCoachMessageRepository coachMessageRepository;
    private final MessageRepository messageRepository;
    private final RelationshipContextService contextService;

    private static final String SYSTEM_PROMPT = """
            You are Aria, the couple's absolute best friend and their relationship's #1 fan. 
            You aren't a robotic coach; you're the person they turn to when they want to celebrate a win, vent about a tiny annoyance, or just feel understood.

            YOUR BEST FRIEND VIBE:
            - **Warm & Casual**: Talk like a real friend. Use "I'm so happy for you guys!" or "Ugh, I totally get why that's frustrating." 
            - **Hyper-Aware**: You know their "Vibe Check" answers and their recent chats. Use that! (e.g., "Since I know you both value [Onboarding Value], maybe try...")
            - **Deeply Supportive**: You never judge. You're always in their corner, rooting for their love.
            - **Open & Honest**: Don't be afraid to be real. If they're drifting, point it out gently like a bestie would.

            YOUR GOALS:
            - Make them feel seen. Reference things they've actually said in their main chat or their Discovery answers.
            - Keep it snappy. No long lectures. Just 2-3 sentences of pure warmth and one tiny "Bestie Tip" to connect.
            - Use their names. Use emojis 💜 ✨ 🫂. 

            CONSTRAINTS:
            - NO generic AI talk. No "As an AI..."
            - Focus on the *them* - the couple. 
            """;

    public String chat(UUID coupleId, UUID userId, String userMessage) {
        // 1. Save user message
        coachMessageRepository.save(AiCoachMessage.builder()
                .coupleId(coupleId)
                .senderId(userId)
                .role("USER")
                .content(userMessage)
                .build());

        // 2. Gather unified relationship intelligence (metrics + vibes)
        String relationshipIntelligence = contextService.getUnifiedContext(coupleId);
        
        // 2b. Get a peek into their recent main chat conversations
        StringBuilder coupleChatSnippet = new StringBuilder();
        messageRepository.findByCoupleIdOrderByCreatedAtDesc(coupleId, 
            PageRequest.of(0, 15))
            .getContent().stream().sorted((a,b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
            .forEach(m -> {
                if (m.getContent() != null && !m.getContent().isEmpty()) {
                    coupleChatSnippet.append(m.getSenderId().equals(userId) ? "User" : "Partner")
                                     .append(": ").append(m.getContent()).append("\n");
                }
            });

        // 3. Build the sophisticated prompt
        StringBuilder finalPrompt = new StringBuilder(SYSTEM_PROMPT);
        finalPrompt.append("\n\n=== RELATIONSHIP INTEL & VIBE CHECK (ONBOARDING) ===\n");
        finalPrompt.append(relationshipIntelligence);

        finalPrompt.append("\n\n=== RECENT COUPLE CHIP-CHAT (WHAT THEY'VE BEEN TALKING ABOUT) ===\n");
        finalPrompt.append(coupleChatSnippet.length() > 0 ? coupleChatSnippet.toString() : "No recent messages yet.");

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
