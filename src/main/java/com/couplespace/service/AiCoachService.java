package com.couplespace.service;

import com.couplespace.entity.*;
import com.couplespace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiCoachService {

    private final OpenAiService openAiService;
    private final AiCoachMessageRepository coachMessageRepository;
    private final MessageRepository messageRepository;
    private final RelationshipContextService contextService;
    private final CoupleService coupleService;
    private final UserRepository userRepository;

    private static final String SYSTEM_PROMPT = """
            You are Aria, the user's absolute best friend and their relationship's #1 fan. 
            You aren't a robotic coach; you're the person they turn to when they want to celebrate a win, vent about a tiny annoyance, or just feel understood.

            YOUR BEST FRIEND VIBE:
            - **Warm & Casual**: Talk like a real friend. Use "I'm so happy for you!" or "Ugh, I totally get why that's frustrating." 
            - **Personal & Intimate**: You know THEIR name and their partner's name. Address them as a close friend would.
            - **Hyper-Aware**: You know their "Vibe Check" answers and their recent chats. Use that!
            - **Loyal & Supportive**: You never judge. You're always in their corner, rooting for their personal growth and their love.
            - **Open & Honest**: Don't be afraid to be real. If they need to hear a hard truth, say it with love.

            YOUR GOALS:
            - Make the user feel seen. Reference things they've actually said or their Discovery answers.
            - Keep it snappy. No long lectures. Just 2-3 sentences of pure warmth and one tiny "Bestie Tip".
            - Use emojis 💜 ✨ 🫂. 

            CONSTRAINTS:
            - YOU ARE TALKING TO ONE SPECIFIC PARTNER PRIVATELY. 
            - DO NOT reveal specific secrets unless they were shared in the main chat.
            - NO generic AI talk. No "As an AI..."
            """;

    @Transactional
    public String chat(UUID coupleId, UUID userId, String userMessage) {
        // 0. Get names for personalization
        User user = userRepository.findById(userId).orElse(null);
        Couple couple = coupleService.getCoupleById(coupleId);
        
        String userName = (user != null) ? user.getName() : "Friend";
        String partnerName = "your partner";
        
        if (couple.getPartner1() != null && !couple.getPartner1().getUserId().equals(userId)) {
            partnerName = couple.getPartner1().getName();
        } else if (couple.getPartner2() != null && !couple.getPartner2().getUserId().equals(userId)) {
            partnerName = couple.getPartner2().getName();
        }

        // 1. Save user message
        coachMessageRepository.save(AiCoachMessage.builder()
                .coupleId(coupleId)
                .userId(userId)
                .senderId(userId)
                .role("USER")
                .content(userMessage)
                .build());

        // 2. Gather unified relationship intelligence (metrics + vibes)
        String relationshipIntelligence = contextService.getUnifiedContext(coupleId);
        
        // 2b. Get a peek into their recent main chat conversations
        StringBuilder coupleChatSnippet = new StringBuilder();
        messageRepository.findByCoupleIdOrderByCreatedAtDesc(coupleId, 
            PageRequest.of(0, 20))
            .getContent().stream().sorted((a,b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
            .forEach(m -> {
                if (m.getContent() != null && !m.getContent().isEmpty()) {
                    String senderName = m.getSenderId().equals(userId) ? userName : partnerName;
                    coupleChatSnippet.append(senderName).append(": ").append(m.getContent()).append("\n");
                }
            });

        // 3. Build the sophisticated prompt
        StringBuilder finalPrompt = new StringBuilder(SYSTEM_PROMPT);
        finalPrompt.append("\n\n=== PERSONAL CONTEXT ===\n");
        finalPrompt.append("User Name: ").append(userName).append("\n");
        finalPrompt.append("Partner Name: ").append(partnerName).append("\n");

        finalPrompt.append("\n\n=== RELATIONSHIP INTEL & VIBE CHECK (ONBOARDING) ===\n");
        finalPrompt.append(relationshipIntelligence);

        finalPrompt.append("\n\n=== RECENT COUPLE MESSAGES (CONTEXT ONLY) ===\n");
        finalPrompt.append(coupleChatSnippet.length() > 0 ? coupleChatSnippet.toString() : "No recent messages yet.");

        finalPrompt.append("\n\n=== RECENT CONVERSATION HISTORY WITH YOU (ARIA) ===\n");
        coachMessageRepository.findTop20ByUserIdOrderByCreatedAtAsc(userId).forEach(msg -> {
            finalPrompt.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        });

        try {
            // 4. Generate AI response
            String reply = openAiService.chatCompletion(finalPrompt.toString(), userMessage);

            // 5. Save and return
            coachMessageRepository.save(AiCoachMessage.builder()
                    .coupleId(coupleId)
                    .userId(userId)
                    .role("ASSISTANT")
                    .content(reply)
                    .build());

            return reply;
        } catch (Exception e) {
            log.error("Aria system error for user {}: {}", userId, e.getMessage());
            return "Hey " + userName + ", I'm momentarily offline, but I'm still here for you. Take a deep breath. I'll be back in just a second. 💜";
        }
    }
}
