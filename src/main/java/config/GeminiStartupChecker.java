package src.main.java.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class GeminiStartupChecker implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(GeminiStartupChecker.class);

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    @Override
    public void run(ApplicationArguments args) {
        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            log.warn("GEMINI_API_KEY is not set — AI matching, summaries, and assessments will use fallback logic only.");
        } else {
            log.info("Gemini API key loaded — AI features are active.");
        }
    }
}
