package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class AiSummaryService {
    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String generateSummary(String skillName, int durationMinutes, String learnerName, String mentorName) {
        String prompt = "Summarize a mentorship session on " + skillName +
                        " lasting " + durationMinutes + " minutes between mentor " + mentorName +
                        " and learner " + learnerName + ". Focus on key takeaways and value exchanged.";
        // Call OpenAI API (simplified)
        // In production, use OpenAI Java client or WebClient
        // Return a mock summary for now
        return "Great session on " + skillName + "! The learner gained practical insights into " +
               skillName + ". Both parties agreed to continue collaboration.";
    }
    
//    public String generateSummary(String skillName, int durationMinutes, String learnerName, String mentorName) {
//        String prompt = "Summarize a mentorship session on " + skillName + "...";
//        // Call OpenAI API
//        // ...
//        return summaryFromOpenAI;
//    }
}