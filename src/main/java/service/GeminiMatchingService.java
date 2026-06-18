package service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GeminiMatchingService {

    private static final Logger log = LoggerFactory.getLogger(GeminiMatchingService.class);

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

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
        List<Long> fallback = mentors.stream().map(m -> m.userId).collect(Collectors.toList());

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set — returning mentors in original order.");
            return fallback;
        }
        if (mentors.isEmpty()) {
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

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                GEMINI_URL + geminiApiKey, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String text = root.path("candidates").get(0)
                                  .path("content").path("parts").get(0)
                                  .path("text").asText("").trim();
                text = text.replaceAll("(?s)```[a-z]*\\n?|```", "").trim();
                return objectMapper.readValue(text, new TypeReference<List<Long>>() {});
            }
        } catch (Exception e) {
            log.error("Gemini API call failed for rankMentors: {}", e.getMessage());
        }

        return fallback;
    }
}
