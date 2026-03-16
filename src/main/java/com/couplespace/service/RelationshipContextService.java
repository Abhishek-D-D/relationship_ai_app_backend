package com.couplespace.service;

import com.couplespace.entity.*;
import com.couplespace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RelationshipContextService {

    private final AiInsightRepository insightRepository;
    private final RelationshipMetricsRepository metricsRepository;
    private final PartnerPersonaRepository personaRepository;
    private final OnboardingResponseRepository onboardingResponseRepository;
    private final OnboardingQuestionRepository onboardingQuestionRepository;
    private final MoodSnapshotRepository moodSnapshotRepository;

    public String getUnifiedContext(UUID coupleId) {
        StringBuilder context = new StringBuilder();
        context.append("=== Core Relationship Context ===\n");

        // 1. Latest Insight/Score
        insightRepository.findTopByCoupleIdOrderByWeekStartDesc(coupleId).ifPresent(insight -> {
            context.append("Current Health Score: ").append(insight.getHealthScore()).append("/100\n");
            context.append("Latest Weekly Summary: ").append(insight.getSummary()).append("\n");
        });

        // 2. Performance Metrics
        metricsRepository.findTopByCoupleIdOrderByPeriodStartDesc(coupleId).ifPresent(m -> {
            context.append("Interaction Volume: ").append(m.getMessageCount()).append(" msgs, ")
                    .append(m.getCallCount()).append(" calls/week\n");
            context.append("Positivity Trend: ").append(m.getPositiveScore()).append("/100\n");
            context.append("Responsiveness: Avg ").append(m.getAvgResponseTimeMins()).append(" mins\n");
        });

        // 3. Partner Personas & Moods
        context.append("\n=== Partner Profiles ===\n");
        personaRepository.findByCoupleId(coupleId).forEach(persona -> {
            context.append("- Partner (").append(persona.getUserId()).append("): ");
            context.append("Style: ").append(persona.getCommunicationStyle());
            context.append(", Love Language: ").append(persona.getPrimaryLoveLanguage());
            context.append(", Vibes: ").append(persona.getAura()).append("\n");

            moodSnapshotRepository.findTopByCoupleIdAndUserIdOrderByCreatedAtDesc(coupleId, persona.getUserId())
                    .ifPresent(mood -> context.append("  Current Mood: ").append(mood.getMood()).append(" (Score: ")
                            .append(mood.getMoodScore()).append(")\n"));
        });

        // 4. Onboarding Values
        context.append("\n=== Foundational Values (Onboarding) ===\n");
        onboardingResponseRepository.findByCoupleId(coupleId).forEach(resp -> {
            onboardingQuestionRepository.findById(resp.getQuestionId()).ifPresent(q -> {
                context.append("Discovery: ").append(q.getQuestionText()).append(" -> ").append(resp.getAnswerText())
                        .append("\n");
            });
        });

        return context.toString();
    }
}
