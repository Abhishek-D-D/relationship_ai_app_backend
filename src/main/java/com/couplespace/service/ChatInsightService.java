package com.couplespace.service;

import com.couplespace.dto.ChatInsightDTO;
import com.couplespace.entity.Couple;
import com.couplespace.entity.Message;
import com.couplespace.entity.User;
import com.couplespace.repository.MessageRepository;
import com.couplespace.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatInsightService {

    private final OpenAiService openAiService;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CoupleService coupleService;
    private final RelationshipContextService contextService;
    private final ObjectMapper objectMapper;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    private final ModerationService moderationService;

    private static final String INSIGHT_PROMPT = """
            You are Aria, the ultimate relationship coach and 'guardian angel' for the user. 
            You are observing a private chat between the user and their partner.
            
            YOUR TASK:
            1. Analyze the partner's latest message and the overall mood of the recent chat.
            2. Provide a 'Mood Suggestion' (e.g., "Your partner seems a bit overwhelmed today").
            3. Provide a brief 'Analytical Insight' explaining why you think so (e.g., "They mentioned work stress twice in the last hour").
            4. Provide 3 'Response Suggestions' for the user. These should be warm, empathetic, and help build a deeper connection.
            
            OUTPUT FORMAT:
            You MUST return a JSON object with this structure:
            {
              "moodSuggestion": "...",
              "analyticalInsight": "...",
              "responseSuggestions": ["...", "...", "..."]
            }
            
            Keep the tone warm, supportive, and wise. Do not use generic AI language. 
            Be specific to the names provided.
            
            SAFETY CONSTRAINTS:
            - NEVER provide sexualized, harmful, or illegal advice.
            - If the chat log contains mentions of self-harm or violence, provide a compassionate response suggesting professional help and do not attempt to analyze the conflict further.
            """;

    @Async
    public void generateInsightForUser(UUID coupleId, UUID userId, Message lastMessage) {
        try {
            // Only generate insights if the last message was from the PARTNER
            if (lastMessage.getSenderId().equals(userId)) {
                return;
            }

            User user = userRepository.findById(userId).orElse(null);
            Couple couple = coupleService.getCoupleById(coupleId);
            if (user == null || couple == null) return;

            String userName = user.getName();
            String partnerName = lastMessage.getSenderId().equals(couple.getPartner1().getUserId()) 
                    ? couple.getPartner1().getName() : couple.getPartner2().getName();

            // Gather context
            String context = contextService.getUnifiedContext(coupleId);
            List<Message> history = messageRepository.findByCoupleIdOrderByCreatedAtDesc(coupleId, PageRequest.of(0, 10))
                    .getContent();
            
            String chatSnippet = history.stream()
                    .sorted((a,b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                    .map(m -> (m.getSenderId().equals(userId) ? userName : partnerName) + ": " + m.getContent())
                    .collect(Collectors.joining("\n"));

            String finalPrompt = INSIGHT_PROMPT + "\n\n" +
                    "Context: " + context + "\n\n" +
                    "Recent Chat:\n" + chatSnippet + "\n\n" +
                    "Generate the JSON insight for " + userName + " regarding " + partnerName + "'s latest message.";

            String aiResponse = openAiService.chatCompletion(finalPrompt, "Generate JSON now.");
            
            // Moderation check
            if (moderationService.isHarmful(aiResponse)) {
                log.warn("Aria insight flagged as harmful for couple {}. Skipping broadcast.", coupleId);
                return;
            }

            // Extract JSON if AI wrapped it in markdown
            if (aiResponse.contains("```json")) {
                aiResponse = aiResponse.substring(aiResponse.indexOf("```json") + 7, aiResponse.lastIndexOf("```"));
            }

            ChatInsightDTO insight = objectMapper.readValue(aiResponse, ChatInsightDTO.class);
            
            // Broadcast to the user via private queue (Aria Insight)
            messagingTemplate.convertAndSendToUser(
                user.getEmail(), 
                "/queue/chat.insight", 
                insight
            );
            
            log.info("Broadcasted Aria Insight to user {}: {}", userId, insight.getMoodSuggestion());
            
        } catch (Exception e) {
            log.error("Failed to generate Aria insight: {}", e.getMessage());
        }
    }
}
