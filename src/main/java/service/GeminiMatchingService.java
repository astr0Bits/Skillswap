package src.main.java.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiMatchingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiMatchingService.class);

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class MentorSummary {
        public Long userId;
        public String name;
        public String location;
        public String skills;
        public double averageRating;
        public int totalSessions;
        public String badges;

        public MentorSummary(Long userId, String name, String location, String skills,
                             double averageRating, int totalSessions, String badges) {
            this.userId = userId;
            this.name = name;
            this.location = location;
            this.skills = skills;
            this.averageRating = averageRating;
            this.totalSessions = totalSessions;
            this.badges = badges;
        }
    }

    public List<Long> rankMentors(String learnerGoals, String currentLevel,
                                  String preferredSchedule, String desiredSkills,
                                  List<MentorSummary> mentors) {
        log.info("[GeminiMatch] rankMentors called — mentorCount={}, desiredSkills='{}', keyBlank={}",
                mentors.size(), desiredSkills, (geminiApiKey == null || geminiApiKey.isBlank()));

        if (mentors.isEmpty()) {
            return List.of();
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            List<Long> fallback = skillAwareFallback(mentors, desiredSkills);
            log.warn("[GeminiMatch] KEY IS BLANK — using skill-aware fallback: {}", fallback);
            return fallback;
        }

        try {
            String mentorsJson = objectMapper.writeValueAsString(mentors);
            String prompt =
                "You are a mentor-matching assistant for SkillSwap, a peer-to-peer skill-sharing platform.\n\n" +
                "A learner has the following profile:\n" +
                "- Learning goals: " + learnerGoals + "\n" +
                "- Current level: " + currentLevel + "\n" +
                "- Preferred schedule: " + preferredSchedule + "\n" +
                "- Skills they want to learn: " + desiredSkills + "\n\n" +
                "Here are the available mentors:\n" + mentorsJson + "\n\n" +
                "Task: Rank these mentors from most to least suitable for this specific learner based on:\n" +
                "1. Skill overlap between what the learner wants and what the mentor teaches\n" +
                "2. Mentor's rating and experience (totalSessions)\n" +
                "3. Any relevant badges\n\n" +
                "Return ONLY a valid JSON array of userId numbers in ranked order, most suitable first.\n" +
                "Example output: [12, 7, 3, 15, 2]\n" +
                "Do not include any text, explanation, or markdown outside the JSON array.";

            log.info("[GeminiMatch] PROMPT SENT TO GEMINI:\n{}", prompt);

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = UriComponentsBuilder.fromHttpUrl(GEMINI_URL)
                    .queryParam("key", geminiApiKey)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            log.info("[GeminiMatch] RAW GEMINI RESPONSE (HTTP {}): {}", response.getStatusCode(), response.getBody());

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String text = root.path("candidates").get(0)
                                  .path("content").path("parts").get(0)
                                  .path("text").asText("").trim();
                log.info("[GeminiMatch] Extracted text from Gemini response: '{}'", text);
                text = text.replaceAll("(?s)```[a-z]*\\n?|```", "").trim();
                List<Long> ranked = objectMapper.readValue(text, new TypeReference<List<Long>>() {});
                log.info("[GeminiMatch] Parsed ranked IDs from Gemini: {}", ranked);
                return ranked;
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("[GeminiMatch] 429 QUOTA EXHAUSTED — will use skill-aware fallback. Google says: {}",
                        e.getResponseBodyAsString());
            } else {
                log.error("[GeminiMatch] Gemini HTTP error {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.error("[GeminiMatch] Gemini call threw exception: {} — {}", e.getClass().getSimpleName(), e.getMessage());
        }

        List<Long> fallback = skillAwareFallback(mentors, desiredSkills);
        log.warn("[GeminiMatch] USING SKILL-AWARE FALLBACK: {}", fallback);
        return fallback;
    }

    private List<Long> skillAwareFallback(List<MentorSummary> mentors, String desiredSkills) {
        List<String> wanted = Arrays.stream(desiredSkills.split(",\\s*"))
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        return mentors.stream()
                .sorted(Comparator
                        .comparingInt((MentorSummary m) -> -skillOverlap(m.skills, wanted))
                        .thenComparingDouble(m -> -m.averageRating)
                        .thenComparingInt(m -> -m.totalSessions))
                .map(m -> m.userId)
                .collect(Collectors.toList());
    }

    private static int skillOverlap(String mentorSkills, List<String> wantedLower) {
        if (mentorSkills == null || mentorSkills.isBlank()) return 0;
        String lower = mentorSkills.toLowerCase();
        int count = 0;
        for (String w : wantedLower) {
            if (lower.contains(w)) count++;
        }
        return count;
    }
}
