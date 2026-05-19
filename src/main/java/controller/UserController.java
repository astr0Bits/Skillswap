package controller;

import dto.UserUpdateDTO;
import dto.BadgeDTO;
import dto.UserStatsDTO;
import model.User;
import repository.AuditLogRepository;
import repository.MentorAvailabilityRepository;
import repository.NotificationRepository;
import repository.ReviewRepository;
import repository.SessionRepository;
import repository.UserPreferencesRepository;
import repository.UserRepository;
import repository.UserSkillRepository;
import service.BadgeService;
import service.UserService;
import service.UserStatsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;



@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UserStatsService userStatsService;
    private final BadgeService badgeService;
    private final SessionRepository sessionRepository;
    private final NotificationRepository notificationRepository;
    private final UserSkillRepository userSkillRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final UserPreferencesRepository userPreferencesRepository;
    private final ReviewRepository reviewRepository;
    private final AuditLogRepository auditLogRepository;

    // FIX: All declared repositories are now properly injected via constructor
    public UserController(UserService userService,
                          PasswordEncoder passwordEncoder,
                          UserRepository userRepository,
                          UserStatsService userStatsService,
                          BadgeService badgeService,
                          SessionRepository sessionRepository,
                          NotificationRepository notificationRepository,
                          UserSkillRepository userSkillRepository,
                          MentorAvailabilityRepository mentorAvailabilityRepository,
                          UserPreferencesRepository userPreferencesRepository,
                          ReviewRepository reviewRepository,
                          AuditLogRepository auditLogRepository) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.userStatsService = userStatsService;
        this.badgeService = badgeService;
        this.sessionRepository = sessionRepository;
        this.notificationRepository = notificationRepository;
        this.userSkillRepository = userSkillRepository;
        this.mentorAvailabilityRepository = mentorAvailabilityRepository;
        this.userPreferencesRepository = userPreferencesRepository;
        this.reviewRepository = reviewRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @PutMapping("/update/{email}")
    public ResponseEntity<?> updateUser(
            @PathVariable String email,
            @Valid @RequestBody UserUpdateDTO updateDTO,
            Authentication auth) {

        if (!auth.getName().equals(email)) {
            return ResponseEntity.status(403).body("You can only update your own profile.");
        }

        try {
            User updatedUser = userService.updateUser(email, updateDTO, passwordEncoder);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PersistenceContext
    private EntityManager entityManager;

    @DeleteMapping("/delete/{email}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable String email, Authentication auth) {

        if (!auth.getName().equals(email)) {
            return ResponseEntity.status(403).body("You are not authorized to delete this account.");
        }

        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(404).body("User not found.");
        }

        Long userId = user.getId();

        // 1. password history — native query since no repo
        entityManager.createNativeQuery(
            "DELETE FROM user_password_history WHERE user_id = :uid")
            .setParameter("uid", userId)
            .executeUpdate();

        // 2. Child tables via repositories
       // auditLogRepository.deleteByUserId(userId);
        notificationRepository.deleteByUserId(userId);
        userSkillRepository.deleteByUserId(userId);
        mentorAvailabilityRepository.deleteByUserId(userId);
        userPreferencesRepository.deleteByUserId(userId);
        reviewRepository.deleteByUserId(userId);
        sessionRepository.deleteByUserId(userId);

        // 3. Finally delete the user
        userRepository.delete(user);

        return ResponseEntity.ok("Account deleted successfully.");
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "name", user.getName(),
                "role", user.getRole().name(),
                "credits", user.getCredits()
        ));
    }

    @GetMapping("/me/badges")
    public ResponseEntity<List<BadgeDTO>> getMyBadges(Authentication auth) {
        User user = getUserFromAuth(auth);
        UserStatsDTO stats = userStatsService.getUserStats(user);
        List<BadgeDTO> badges = badgeService.getUserBadges(user, stats.getPoints(), stats.getTotalSessions(), stats.getReputation());
        return ResponseEntity.ok(badges);
    }

    @GetMapping("/me/earnings")
    public ResponseEntity<Map<String, Object>> getMentorEarnings(Authentication auth) {
        User user = getUserFromAuth(auth);
        long completedMentorSessions = sessionRepository.countCompletedMentorSessions(user);
        int totalCreditsEarned = (int) completedMentorSessions * 3;
        Map<String, Object> response = new HashMap<>();
        response.put("totalCreditsEarned", totalCreditsEarned);
        response.put("completedSessionsAsMentor", completedMentorSessions);
        response.put("thisMonthCredits", 0);
        response.put("monthlyBreakdown", List.of());
        return ResponseEntity.ok(response);
    }

    // Helper method to extract User from Authentication
    private User getUserFromAuth(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}