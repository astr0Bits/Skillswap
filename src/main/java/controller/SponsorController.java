package controller;

import model.User;
import repository.SessionRepository;
import repository.UserRepository;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sponsors")
@CrossOrigin(origins = "*")
public class SponsorController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public SponsorController(UserRepository userRepository, SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    @GetMapping("/me/stats")
    public ResponseEntity<Map<String, Object>> getSponsorStats(Authentication auth) {
        User sponsor = getUserFromAuth(auth);
        // For demo purposes, generate realistic placeholder stats.
        // In a real implementation, you would query programs, allocations, etc.
        Map<String, Object> stats = new HashMap<>();
        stats.put("learnersSponsored", 24);
        stats.put("creditsAllocated", 1250);
        stats.put("skillsCovered", 18);
        stats.put("sessionsFunded", 87);
        stats.put("aedSaved", "12,500 AED");
        stats.put("activePrograms", 3);
        
        // Example programs
        stats.put("programs", List.of(
            Map.of("name", "Tech Scholarship", "type", "scholarship", "description", "Full-stack development", "active", true),
            Map.of("name", "Winter Coupons", "type", "coupon", "description", "50% off Python courses", "active", true),
            Map.of("name", "AI Internship", "type", "internship", "description", "3-month program", "active", false)
        ));
        
        // Example top learners (talent pool)
        stats.put("topLearners", List.of(
            Map.of("name", "Aisha K.", "skill", "Data Science", "sessionsCompleted", 12, "rating", 4.8),
            Map.of("name", "Omar R.", "skill", "JavaScript", "sessionsCompleted", 8, "rating", 4.9)
        ));
        
        return ResponseEntity.ok(stats);
    }

    private User getUserFromAuth(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}