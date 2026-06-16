package controller;

import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import repository.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('ADMIN')")
public class UserManagementController {

    private final UserRepository userRepo;
    private final SessionRepository sessionRepo;
    private final ReviewRepository reviewRepo;
    private final UserSkillRepository userSkillRepo;
    private final UserPreferencesRepository userPrefsRepo;

    public UserManagementController(UserRepository userRepo,
                                    SessionRepository sessionRepo,
                                    ReviewRepository reviewRepo,
                                    UserSkillRepository userSkillRepo,
                                    UserPreferencesRepository userPrefsRepo) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.reviewRepo = reviewRepo;
        this.userSkillRepo = userSkillRepo;
        this.userPrefsRepo = userPrefsRepo;
    }

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(
            userRepo.findAll().stream().map(u -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", u.getId());
                m.put("name", u.getName());
                m.put("email", u.getEmail());
                m.put("role", u.getRole() != null ? u.getRole().name() : "");
                m.put("enabled", u.isEnabled());
                m.put("credits", u.getCredits() != null ? u.getCredits() : 0);
                m.put("reputation", u.getReputation() != null ? u.getReputation() : 0);
                m.put("location", u.getLocation() != null ? u.getLocation() : "");
                return m;
            }).collect(Collectors.toList())
        );
    }

    @PutMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(@PathVariable Long id) {
        User user = userRepo.findById(id).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        user.setEnabled(!user.isEnabled());
        userRepo.save(user);
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "enabled", user.isEnabled(),
            "message", user.isEnabled() ? "User enabled" : "User disabled"
        ));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepo.existsById(id)) return ResponseEntity.notFound().build();
        // Delete child records before removing the user to satisfy FK constraints.
        // Each step is isolated so a buggy native query doesn't abort the whole cascade.
        try { userPrefsRepo.deleteByUserId(id); } catch (Exception ignored) {}
        try { userSkillRepo.deleteByUserId(id); } catch (Exception ignored) {}
        try { reviewRepo.deleteByUserId(id); } catch (Exception ignored) {}
        try { sessionRepo.deleteByUserId(id); } catch (Exception ignored) {}
        userRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }
}
