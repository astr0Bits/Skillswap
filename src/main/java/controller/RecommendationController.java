// controller/RecommendationController.java
package controller;

import dto.RecommendationDTO;
import dto.UserPreferencesDTO;
import model.User;
import repository.UserRepository;
import service.RecommendationService;
import service.UserPreferencesService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recommendations/me")
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final UserPreferencesService preferencesService;
    private final UserRepository userRepository;

    public RecommendationController(RecommendationService recommendationService,
                                    UserPreferencesService preferencesService,
                                    UserRepository userRepository) {
        this.recommendationService = recommendationService;
        this.preferencesService = preferencesService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(Authentication auth) {
        User user = getUser(auth);
        UserPreferencesDTO prefs = preferencesService.getPreferences(user);
        return ResponseEntity.ok(recommendationService.getRecommendations(user, prefs.getLearningGoals()));
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}