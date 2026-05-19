// controller/BadgeController.java
package controller;

import dto.BadgeDTO;
import dto.UserStatsDTO;
import model.User;
import repository.UserRepository;
import service.BadgeService;
import service.UserStatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/badges/me")
public class BadgeController {
    private final UserStatsService statsService;
    private final BadgeService badgeService;
    private final UserRepository userRepository;

    public BadgeController(UserStatsService statsService, BadgeService badgeService, UserRepository userRepository) {
        this.statsService = statsService;
        this.badgeService = badgeService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<BadgeDTO>> getBadges(Authentication auth) {
        User user = getUser(auth);
        UserStatsDTO stats = statsService.getUserStats(user);
        List<BadgeDTO> badges = badgeService.getUserBadges(user, stats.getPoints(), stats.getTotalSessions(), stats.getReputation());
        return ResponseEntity.ok(badges);
    }

    private User getUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}