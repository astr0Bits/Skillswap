package service;

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

import java.util.List;
import java.util.Map;

@Service
public class AiSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiSummaryService.class);

    private static final String GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash-lite:generateContent";

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateSummary(String skillName, int durationMinutes,
                                  String learnerName, String mentorName) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set — returning fallback summary.");
            return "Summary unavailable — please check back later.";
        }

        String prompt = "You are an educational assistant. Summarize the following mentorship session " +
                "in 3-5 sentences, highlighting the key topics covered, skills practised, and value " +
                "gained by the learner.\n\n" +
                "Session details:\n" +
                "- Skill: " + skillName + "\n" +
                "- Duration: " + durationMinutes + " minutes\n" +
                "- Mentor: " + mentorName + "\n" +
                "- Learner: " + learnerName + "\n\n" +
                "Write in a clear, encouraging tone suitable for a learner to review later as revision notes.";

        try {
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

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                String text = root.path("candidates").get(0)
                                  .path("content").path("parts").get(0)
                                  .path("text").asText("");
                if (!text.isBlank()) {
                    return text;
                }
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Gemini quota exhausted for generateSummary (429) — returning fallback.");
            } else {
                log.error("Gemini API error for generateSummary: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            }
        } catch (Exception e) {
            log.error("Gemini API call failed for generateSummary: {}", e.getMessage());
        }

        return "Summary unavailable — please check back later.";
    }
}
