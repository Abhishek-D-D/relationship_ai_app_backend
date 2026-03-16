package com.couplespace.service;

import com.couplespace.entity.RelationshipMilestone;
import com.couplespace.repository.MessageRepository;
import com.couplespace.repository.RelationshipMilestoneRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.couplespace.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class TimelineService {

    private final MessageRepository messageRepository;
    private final RelationshipMilestoneRepository milestoneRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    public List<RelationshipMilestone> getTimeline(UUID coupleId) {
        return milestoneRepository.findByCoupleIdOrderByMilestoneDateAsc(coupleId);
    }

    @Async
    @Transactional
    public void extractMilestones(UUID coupleId) {
        log.info("Starting timeline extraction for couple {}", coupleId);

        // Fetch last 200 messages for rich context
        var messages = messageRepository.findByCoupleIdOrderByCreatedAtAsc(coupleId, PageRequest.of(0, 200));

        if (messages.size() < 5) {
            log.info("Not enough messages to extract milestones for couple {}", coupleId);
            return;
        }

        // Fetch sender names for context
        List<UUID> senderIds = messages.stream().map(com.couplespace.entity.Message::getSenderId).distinct().toList();
        Map<UUID, String> nameMap = userRepository.findAllById(senderIds).stream()
                .collect(
                        Collectors.toMap(com.couplespace.entity.User::getUserId, com.couplespace.entity.User::getName));

        // Build compact chat log for LLM
        String chatLog = messages.stream()
                .filter(m -> m.getContent() != null && !m.getContent().isBlank())
                .map(m -> "[%s] %s: %s".formatted(
                        m.getCreatedAt().toLocalDate(),
                        nameMap.getOrDefault(m.getSenderId(), "Partner"),
                        m.getContent()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = """
                You are an expert relationship analyst and storyteller. Your task is to identify meaningful milestones
                in a couple's chat history and create cinematic, emotional memory summaries for each one.
                Always respond with ONLY valid JSON — no markdown, no explanation.""";

        String userPrompt = """
                Analyze this couple's chat history and extract 5-10 meaningful milestone moments.
                Look for: first deep conversations, declarations of love, resolved conflicts, exciting moments,
                emotional breakthroughs, funny shared moments, plans made together, supportive exchanges.

                Chat log:
                %s

                Return a JSON array where each item has:
                - "title": a short evocative title (e.g., "The First 'I Love You'", "A Night of Laughter")
                - "milestoneDate": date in YYYY-MM-DD format (pick the closest date from the context)
                - "milestoneType": one of [ROMANTIC, ACHIEVEMENT, FUNNY, EMOTIONAL, CONFLICT_RESOLVED, GROWTH, FIRST]
                - "memorySummary": 2-3 warm, vivid sentences describing this moment as if writing in a relationship journal

                Return ONLY the JSON array, example:
                [{"title":"...","milestoneDate":"2025-01-15","milestoneType":"ROMANTIC","memorySummary":"..."}]
                """
                .formatted(chatLog.length() > 6000 ? chatLog.substring(chatLog.length() - 6000) : chatLog);

        try {
            String response = openAiService.chatCompletion(systemPrompt, userPrompt);
            String jsonStr = extractJsonArray(response);
            JsonNode milestones = objectMapper.readTree(jsonStr);

            if (!milestones.isArray()) {
                log.warn("LLM did not return an array for couple {}", coupleId);
                return;
            }

            // Clear existing AI-generated milestones & replace with fresh extraction
            milestoneRepository.deleteByCoupleId(coupleId);

            List<RelationshipMilestone> toSave = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;

            for (JsonNode node : milestones) {
                try {
                    LocalDate date = LocalDate.parse(
                            node.path("milestoneDate").asText(LocalDate.now().toString()), formatter);

                    RelationshipMilestone milestone = RelationshipMilestone.builder()
                            .coupleId(coupleId)
                            .title(node.path("title").asText("A Special Moment"))
                            .memorySummary(node.path("memorySummary").asText(""))
                            .milestoneDate(date)
                            .milestoneType(node.path("milestoneType").asText("MOMENT"))
                            .build();
                    toSave.add(milestone);
                } catch (Exception e) {
                    log.warn("Failed to parse milestone node: {}", e.getMessage());
                }
            }

            milestoneRepository.saveAll(toSave);
            log.info("Extracted {} milestones for couple {}", toSave.size(), coupleId);

        } catch (Exception e) {
            log.error("Timeline extraction failed for couple {}: {}", coupleId, e.getMessage());
        }
    }

    private String extractJsonArray(String raw) {
        if (raw == null)
            return "[]";
        int start = raw.indexOf('[');
        int end = raw.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return raw.substring(start, end + 1);
        }
        return "[]";
    }
}
