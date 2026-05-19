package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
public class ZoomService {

    @Value("${zoom.account.id}")
    private String accountId;

    @Value("${zoom.client.id}")
    private String clientId;

    @Value("${zoom.client.secret}")
    private String clientSecret;

    private String accessToken;
    private Instant tokenExpiry;

    private synchronized String getAccessToken() {
        if (accessToken != null && tokenExpiry != null && tokenExpiry.isAfter(Instant.now())) {
            return accessToken;
        }
        WebClient client = WebClient.create("https://zoom.us/oauth/token");
        // Use Map<String, Object> because response contains numbers
        Map<String, Object> response = client.post()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("grant_type", "account_credentials")
                        .queryParam("account_id", accountId)
                        .build())
                .headers(headers -> headers.setBasicAuth(clientId, clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        
        accessToken = (String) response.get("access_token");
        // expires_in is an Integer, cast safely
        Number expiresInNumber = (Number) response.get("expires_in");
        int expiresIn = expiresInNumber.intValue();
        tokenExpiry = Instant.now().plusSeconds(expiresIn - 60);
        return accessToken;
    }

    public String createMeeting(String topic, LocalDateTime startTime, int durationMinutes) {
        String accessToken = getAccessToken();
        WebClient client = WebClient.create("https://api.zoom.us/v2");
        Map<String, Object> request = Map.of(
                "topic", topic,
                "type", 2,
                "start_time", startTime.atZone(ZoneOffset.UTC).toString(),
                "duration", durationMinutes,
                "timezone", "UTC",
                "settings", Map.of("join_before_host", true, "host_video", true, "participant_video", true)
        );
        Map<String, Object> response = client.post()
                .uri("/users/me/meetings")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return (String) response.get("join_url");
    }
}