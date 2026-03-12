package com.couplespace.service;

import com.couplespace.entity.*;
import com.couplespace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OnboardingService {

    private final OnboardingQuestionRepository questionRepository;
    private final OnboardingResponseRepository responseRepository;
    private final PartnerPersonaRepository personaRepository;
    private final OpenAiService openAiService;

    public List<OnboardingQuestion> getAllQuestions() {
        return questionRepository.findAll();
    }

    @Transactional
    public void submitResponses(UUID userId, UUID coupleId, List<OnboardingResponse> responses) {
        responses.forEach(r -> {
            r.setUserId(userId);
            r.setCoupleId(coupleId);
        });
        responseRepository.saveAll(responses);

        // Analyze vibe immediately for this user
        analyzeVibe(userId, coupleId);
    }

    private void analyzeVibe(UUID userId, UUID coupleId) {
        List<OnboardingResponse> userResponses = responseRepository.findByUserId(userId);
        List<OnboardingQuestion> questions = questionRepository.findAll();

        String factStream = userResponses.stream()
                .map(r -> {
                    String qText = questions.stream()
                            .filter(q -> q.getId().equals(r.getQuestionId()))
                            .map(OnboardingQuestion::getQuestionText)
                            .findFirst().orElse("");
                    return "Q: " + qText + "\nA: " + r.getAnswerText();
                })
                .collect(Collectors.joining("\n\n"));

        String prompt = """
                Analyze the following onboarding relationship questionnaire responses for a user.
                Based on these answers, determine the user's:
                1. Communication Style (Short name like 'Explainer', 'Avoider', 'Softener')
                2. Primary Love Language
                3. Aura (A poetic 2-word description of their energy)
                4. Three defining traits (comma separated)

                Responses:
                %s

                Return JSON only:
                {
                  "communicationStyle": "...",
                  "primaryLoveLanguage": "...",
                  "aura": "...",
                  "traits": "..."
                }
                """.formatted(factStream);

        try {
            String json = openAiService.chatCompletion("Return JSON only.", prompt);

            PartnerPersona persona = personaRepository.findByCoupleId(coupleId).stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .findFirst()
                    .orElse(PartnerPersona.builder()
                            .userId(userId)
                            .coupleId(coupleId)
                            .build());

            persona.setCommunicationStyle(parseValue(json, "communicationStyle"));
            persona.setPrimaryLoveLanguage(parseValue(json, "primaryLoveLanguage"));
            persona.setAura(parseValue(json, "aura"));
            persona.setTraits(parseValue(json, "traits"));

            personaRepository.save(persona);
            log.info("Bootstrapped persona for user {}: {}", userId, persona.getAura());

        } catch (Exception e) {
            log.error("Failed to analyze onboarding vibe: {}", e.getMessage());
        }
    }

    private String parseValue(String json, String key) {
        try {
            int start = json.indexOf("\"" + key + "\":") + key.length() + 4;
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
