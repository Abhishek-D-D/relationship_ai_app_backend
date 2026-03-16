package com.couplespace.service;

import com.couplespace.entity.MoodSnapshot;
import com.couplespace.entity.User;
import com.couplespace.repository.MoodSnapshotRepository;
import com.couplespace.repository.MessageRepository;
import com.couplespace.repository.UserRepository;
import com.couplespace.service.CoupleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoodAnalysisService {

    private final MessageRepository messageRepository;
    private final MoodSnapshotRepository moodSnapshotRepository;
    private final CoupleService coupleService;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;
    private final FcmNotificationService fcmNotificationService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * Runs every 12 hours for all connected couples.
     */
    @Scheduled(fixedDelay = 43_200_000)
    @Transactional
    public void runScheduledMoodScan() {
        log.info("Starting scheduled 12-hour mood scan...");
        var allCouples = coupleService.getAllCompleteCouples();
        allCouples.forEach(couple -> {
            try {
                analyseMoodForCouple(couple.getCoupleId(), couple.getPartner1(), couple.getPartner2());
                analyseMoodForCouple(couple.getCoupleId(), couple.getPartner2(), couple.getPartner1());
            } catch (Exception e) {
                log.error("Mood scan failed for couple {}: {}", couple.getCoupleId(), e.getMessage());
            }
        });
        log.info("Mood scan complete for {} couples.", allCouples.size());
    }

    /**
     * Manually trigger mood analysis for a specific couple.
     */
    @Transactional
    public MoodSnapshot analyseMoodForUser(UUID coupleId, UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();
        var couple = coupleService.getCoupleById(coupleId);
        User partner = couple.getPartner1().getUserId().equals(userId)
                ? couple.getPartner2()
                : couple.getPartner1();
        analyseMoodForCouple(coupleId, user, partner);
        return moodSnapshotRepository
                .findTopByCoupleIdAndUserIdOrderByCreatedAtDesc(coupleId, userId)
                .orElseThrow();
    }

    private void analyseMoodForCouple(UUID coupleId, User analysed, User notified) {
        if (analysed == null || notified == null)
            return;

        // Get messages from last 24 hours
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        var recentMessages = messageRepository.findRecentBySender(coupleId, analysed.getUserId(), since);

        if (recentMessages.isEmpty()) {
            log.debug("No recent messages from {} to analyse", analysed.getName());
            return;
        }

        String chatSample = recentMessages.stream()
                .limit(30)
                .map(com.couplespace.entity.Message::getContent)
                .collect(Collectors.joining("\n"));

        try {
            String prompt = """
                    Analyze the following recent chat messages from a partner in a relationship.
                    Detect their subtle emotional state and current vibe.

                    Messages:
                    %s

                    Return ONLY valid JSON:
                    {
                      "mood": "STRESSED | HAPPY | ROMANTIC | DISTANT | ANXIOUS | EXCITED | NEUTRAL",
                      "moodScore": 0-100,
                      "moodNote": "A warm, deeply empathetic one-sentence description for their partner."
                    }
                    """.formatted(chatSample);

            String response = openAiService.chatCompletion(
                    "You are a master of emotional intelligence. Detect subtle relationship dynamics. Return ONLY valid JSON.",
                    prompt);

            var node = objectMapper.readTree(extractJson(response));

            MoodSnapshot snapshot = MoodSnapshot.builder()
                    .coupleId(coupleId)
                    .userId(analysed.getUserId())
                    .mood(node.path("mood").asText("NEUTRAL"))
                    .moodScore(node.path("moodScore").asInt(50))
                    .moodNote(node.path("moodNote").asText(""))
                    .build();

            moodSnapshotRepository.save(snapshot);

            // Send push notification to partner
            if (notified.getFcmToken() != null && !notified.getFcmToken().isBlank()) {
                String title = getMoodTitle(snapshot.getMood(), analysed.getName());
                String body = buildNotificationBody(snapshot.getMood(), analysed.getName(), snapshot.getMoodNote());
                fcmNotificationService.sendNotification(notified.getFcmToken(), title, body);
                snapshot.setNotificationSent(true);
                moodSnapshotRepository.save(snapshot);
            }

        } catch (Exception e) {
            log.error("Mood analysis failed for user {}: {}", analysed.getName(), e.getMessage());
        }
    }

    private String getMoodTitle(String mood, String partnerName) {
        return switch (mood.toUpperCase()) {
            case "STRESSED" -> "💛 " + partnerName + " might need some extra love";
            case "HAPPY" -> "💚 " + partnerName + " is glowing today!";
            case "ROMANTIC" -> "💜 " + partnerName + " is feeling loving";
            case "DISTANT" -> "🤍 A gentle check-in for " + partnerName;
            case "ANXIOUS" -> "🧡 " + partnerName + " seems a bit anxious";
            case "EXCITED" -> "✨ " + partnerName + " is feeling energized!";
            default -> "💙 A moment of connection with " + partnerName;
        };
    }

    private String buildNotificationBody(String mood, String name, String note) {
        String suggestion = switch (mood.toUpperCase()) {
            case "STRESSED" -> "A quick 'thinking of you' text would mean a lot 💜";
            case "ROMANTIC" -> "Match their energy with something sweet! 💕";
            case "DISTANT" -> "Maybe some quality time tonight? ☕️";
            case "ANXIOUS" -> "A little reassurance can go a long way 🌸";
            case "HAPPY", "EXCITED" -> "Celebrate their high vibes together! 🎉";
            default -> "Take a second to say hi 💙";
        };
        return (note == null || note.isBlank() ? "" : note + " ") + suggestion;
    }

    private String extractJson(String raw) {
        if (raw == null)
            return "{}";
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start >= 0 && end > start)
            return raw.substring(start, end + 1);
        return "{}";
    }
}
