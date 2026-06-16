package controller;

import dto.BookSessionRequest;
import enums.SessionMode;
import enums.SessionStatus;
import model.MentorAvailability;
import model.Session;
import model.Skill;
import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.*;
import service.SessionEmailService;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
public class SessionBookingController {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final SkillRepository skillRepository;
    private final MentorAvailabilityRepository availabilityRepository;
    private final SessionEmailService emailService;

    public SessionBookingController(UserRepository userRepository,
                                    SessionRepository sessionRepository,
                                    SkillRepository skillRepository,
                                    MentorAvailabilityRepository availabilityRepository,
                                    SessionEmailService emailService) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.skillRepository = skillRepository;
        this.availabilityRepository = availabilityRepository;
        this.emailService = emailService;
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookSession(Authentication authentication,
                                          @RequestBody BookSessionRequest request) {
        // Guard: authentication must be present (set by AuthTokenFilter via JWT)
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }

        // Resolve learner from JWT principal — never accept learner_id from the request body
        String email = authentication.getName();
        User learner = userRepository.findByEmail(email).orElse(null);
        if (learner == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authenticated user not found"));
        }

        // Validate mentor
        if (request.getMentorId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "mentorId is required"));
        }
        User mentor = userRepository.findById(request.getMentorId()).orElse(null);
        if (mentor == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mentor not found"));
        }
        if (learner.getId().equals(mentor.getId())) {
            return ResponseEntity.badRequest().body(Map.of("error", "You cannot book a session with yourself"));
        }

        // Resolve scheduled time (required for availability check)
        LocalDateTime scheduledTime = request.getScheduledTime();
        if (scheduledTime == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduledTime is required (ISO-8601, e.g. 2026-06-20T10:00:00)"));
        }
        if (scheduledTime.isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduledTime must be in the future"));
        }

        // Validate that the requested time falls within one of the mentor's availability slots
        if (!isWithinMentorAvailability(mentor, scheduledTime)) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Requested time slot is outside mentor availability"));
        }

        // Resolve skill (optional — nullable in DB)
        Skill skill = null;
        if (request.getSkillId() != null) {
            skill = skillRepository.findById(request.getSkillId()).orElse(null);
        }
        if (skill == null && request.getSkillName() != null) {
            skill = skillRepository.findByNameIgnoreCase(request.getSkillName()).orElse(null);
        }

        // Build and persist session
        boolean isOnline = !"in-person".equalsIgnoreCase(request.getMode());
        String meetingLink = isOnline
            ? "https://teams.microsoft.com/l/meetup-join/skillswap_"
              + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
            : null;

        Session session = new Session();
        session.setMentor(mentor);
        session.setLearner(learner);   // always set from JWT — never null
        session.setSkill(skill);
        session.setMode(isOnline ? SessionMode.ONLINE : SessionMode.IN_PERSON);
        session.setStatus(SessionStatus.PENDING);
        session.setScheduledTime(scheduledTime);
        session.setDurationMinutes(60);
        session.setMeetingLink(meetingLink);
        session.setLocation(isOnline ? null : mentor.getLocation());

        try {
            Session saved = sessionRepository.save(session);

            try {
                String skillName = skill != null ? skill.getName()
                    : (request.getSkillName() != null ? request.getSkillName() : "Session");
                emailService.sendBookingConfirmation(learner, mentor, saved, skillName);
            } catch (Exception emailEx) {
                // Email failure must NOT roll back the booking
                System.err.println("=== EMAIL FAILED: " + emailEx.getMessage());
            }

            return ResponseEntity.ok(Map.of(
                "message", "Session booked successfully",
                "sessionId", saved.getId(),
                "mode", isOnline ? "ONLINE" : "IN_PERSON",
                "scheduledTime", saved.getScheduledTime().toString(),
                "meetingLink", meetingLink != null ? meetingLink : ""
            ));

        } catch (Exception e) {
            System.err.println("=== BOOKING FAILED: " + e.getMessage());
            return ResponseEntity.status(500)
                .body(Map.of("error", "Booking failed: " + e.getMessage()));
        }
    }

    /**
     * Returns true if the requested scheduledTime falls within any of the mentor's
     * declared availability slots (recurring by day-of-week, or a one-off specific date).
     * When the mentor has no availability records at all, booking is allowed (open availability).
     */
    private boolean isWithinMentorAvailability(User mentor, LocalDateTime scheduledTime) {
        List<MentorAvailability> slots = availabilityRepository.findByMentor(mentor);
        if (slots.isEmpty()) {
            return true; // no constraints set — treat as always available
        }

        String requestedDay = scheduledTime.getDayOfWeek().name(); // e.g. "MONDAY"
        LocalTime requestedTime = scheduledTime.toLocalTime();

        for (MentorAvailability slot : slots) {
            boolean dayMatches;
            if (slot.isRecurring()) {
                dayMatches = requestedDay.equalsIgnoreCase(slot.getDayOfWeek());
            } else {
                // One-off date: match the exact calendar date
                dayMatches = slot.getSpecificDate() != null
                    && slot.getSpecificDate().equals(scheduledTime.toLocalDate());
            }

            if (dayMatches) {
                LocalTime start = slot.getStartTime();
                LocalTime end   = slot.getEndTime();
                if (start != null && end != null
                        && !requestedTime.isBefore(start)
                        && requestedTime.isBefore(end)) {
                    return true;
                }
            }
        }
        return false;
    }
}
//package controller;
//
//import dto.BookSessionRequest;
//import enums.SessionMode;
//import enums.SessionStatus;
//import model.Session;
//import model.Skill;
//import model.User;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.*;
//import repository.*;
//import service.SessionEmailService;
//
//import java.time.LocalDateTime;
//import java.util.Map;
//import java.util.UUID;
//
//@RestController
//@RequestMapping("/api/sessions")
//public class SessionBookingController {
//
//    private final UserRepository userRepository;
//    private final SessionRepository sessionRepository;
//    private final SkillRepository skillRepository;
//    private final SessionEmailService emailService;
//
//    public SessionBookingController(UserRepository userRepository,
//                                    SessionRepository sessionRepository,
//                                    SkillRepository skillRepository,
//                                    SessionEmailService emailService) {
//        this.userRepository = userRepository;
//        this.sessionRepository = sessionRepository;
//        this.skillRepository = skillRepository;
//        this.emailService = emailService;
//    }
//
//    @PostMapping("/book")
//    public ResponseEntity<?> bookSession(Authentication authentication,
//                                          @RequestBody BookSessionRequest request) {
//        try {
//            String email = authentication.getName();
//            User learner = userRepository.findByEmail(email).orElse(null);
//            if (learner == null) 
//                return ResponseEntity.status(401).body(Map.of("message", "Not authenticated"));
//
//            User mentor = userRepository.findById(request.getMentorId()).orElse(null);
//            if (mentor == null) 
//                return ResponseEntity.badRequest().body(Map.of("message", "Mentor not found"));
//
//            if (learner.getId().equals(mentor.getId()))
//                return ResponseEntity.badRequest().body(Map.of("message", "You cannot book yourself"));
//
//            Skill skill = null;
//            if (request.getSkillId() != null)
//                skill = skillRepository.findById(request.getSkillId()).orElse(null);
//            if (skill == null && request.getSkillName() != null)
//                skill = skillRepository.findByNameIgnoreCase(request.getSkillName()).orElse(null);
//
//            boolean isOnline = !"in-person".equalsIgnoreCase(request.getMode());
//            String meetingLink = isOnline
//                ? "https://teams.microsoft.com/l/meetup-join/skillswap_"
//                  + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
//                : null;
//
//            LocalDateTime scheduledTime = LocalDateTime.now()
//                .plusDays(3).withHour(10).withMinute(0).withSecond(0).withNano(0);
//
//            Session session = new Session();
//            session.setMentor(mentor);
//            session.setLearner(learner);
//            session.setSkill(skill);
//            session.setMode(isOnline ? SessionMode.ONLINE : SessionMode.IN_PERSON);
//            session.setStatus(SessionStatus.PENDING);
//            session.setScheduledTime(scheduledTime);
//            session.setDurationMinutes(60);
//            session.setMeetingLink(meetingLink);
//            session.setLocation(isOnline ? null : mentor.getLocation());
//            session.setCreatedAt(LocalDateTime.now());
//            session.setUpdatedAt(LocalDateTime.now());
//            
//            
//            
//            Session saved = sessionRepository.save(session);
//
//            System.out.println("=== SESSION SAVED ID: " + saved.getId());
//
//            try {
//                String skillName = request.getSkillName() != null ? request.getSkillName()
//                                 : (skill != null ? skill.getName() : "Session");
//                emailService.sendBookingConfirmation(learner, mentor, saved, skillName);
//                System.out.println("=== EMAIL SENT to " + learner.getEmail());
//            } catch (Exception e) {
//                // Email failure must NOT fail the booking
//                System.err.println("=== EMAIL FAILED: " + e.getMessage());
//                e.printStackTrace();
//            }
//
//            return ResponseEntity.ok(Map.of(
//                "message", "Session booked! Confirmation email sent.",
//                "sessionId", saved.getId(),
//                "mode", isOnline ? "ONLINE" : "IN_PERSON",
//                "meetingLink", meetingLink != null ? meetingLink : ""
//            ));
//
//        } catch (Exception e) {
//            System.err.println("=== BOOKING FAILED: " + e.getMessage());
//            e.printStackTrace();
//            return ResponseEntity.status(500)
//                .body(Map.of("message", "Booking failed: " + e.getMessage()));
//        }
//    }
//}