package controller;

import dto.BookSessionRequest;
import enums.SessionMode;
import enums.SessionStatus;
import model.Session;
import model.Skill;
import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.*;
import service.SessionEmailService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionBookingController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SkillRepository skillRepository;
    private final SessionEmailService emailService;

    public SessionBookingController(UserRepository userRepository,
                                    SessionRepository sessionRepository,
                                    SkillRepository skillRepository,
                                    SessionEmailService emailService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.skillRepository = skillRepository;
        this.emailService = emailService;
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookSession(Authentication authentication,
                                          @RequestBody BookSessionRequest request) {
        try {
            String email = authentication.getName();
            User learner = userRepository.findByEmail(email).orElse(null);
            if (learner == null) 
                return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));

            User mentor = userRepository.findById(request.getMentorId()).orElse(null);
            if (mentor == null) 
                return ResponseEntity.badRequest().body(Map.of("message", "Mentor not found"));

            if (learner.getId().equals(mentor.getId()))
                return ResponseEntity.badRequest().body(Map.of("message", "You cannot book yourself"));

            Skill skill = null;
            if (request.getSkillId() != null)
                skill = skillRepository.findById(request.getSkillId()).orElse(null);
            if (skill == null && request.getSkillName() != null)
                skill = skillRepository.findByNameIgnoreCase(request.getSkillName()).orElse(null);

            boolean isOnline = !"in-person".equalsIgnoreCase(request.getMode());
            String meetingLink = isOnline
                ? "https://teams.microsoft.com/l/meetup-join/skillswap_"
                  + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
                : null;

            LocalDateTime scheduledTime = LocalDateTime.now()
                .plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);

            Session session = new Session();
            session.setMentor(mentor);
            session.setLearner(learner);
            session.setSkill(skill);
            session.setMode(isOnline ? SessionMode.ONLINE : SessionMode.IN_PERSON);
            session.setStatus(SessionStatus.PENDING);
            session.setScheduledTime(scheduledTime);
            session.setDurationMinutes(60);
            session.setMeetingLink(meetingLink);
            session.setLocation(isOnline ? null : mentor.getLocation());
            session.setCreatedAt(LocalDateTime.now());
            session.setUpdatedAt(LocalDateTime.now());
            
            
            
            Session saved = sessionRepository.save(session);

            System.out.println("=== SESSION SAVED ID: " + saved.getId());

            try {
                String skillName = request.getSkillName() != null ? request.getSkillName()
                                 : (skill != null ? skill.getName() : "Session");
                emailService.sendBookingConfirmation(learner, mentor, saved, skillName);
                System.out.println("=== EMAIL SENT to " + learner.getEmail());
            } catch (Exception e) {
                // Email failure must NOT fail the booking
                System.err.println("=== EMAIL FAILED: " + e.getMessage());
                e.printStackTrace();
            }

            return ResponseEntity.ok(Map.of(
                "message", "Session booked! Confirmation email sent.",
                "sessionId", saved.getId(),
                "mode", isOnline ? "ONLINE" : "IN_PERSON",
                "meetingLink", meetingLink != null ? meetingLink : ""
            ));

        } catch (Exception e) {
            System.err.println("=== BOOKING FAILED: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500)
                .body(Map.of("message", "Booking failed: " + e.getMessage()));
        }
    }
}