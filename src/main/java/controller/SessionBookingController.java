package controller;

import dto.BookSessionRequest;
import enums.SessionMode;
import enums.SessionStatus;
import exception.AvailabilityException;
import exception.ResourceNotFoundException;
import model.MentorAvailability;
import model.Session;
import model.Skill;
import model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import repository.*;
import security.UserDetailsImpl;
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
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        Long learnerId = principal.getId();
        User learner = userRepository.findById(learnerId)
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));

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

        // Validate that the full 60-min session fits within one of the mentor's availability slots
        if (!isWithinMentorAvailability(mentor, scheduledTime, 60)) {
            throw new AvailabilityException("Requested time slot is outside mentor availability");
        }

        // Reject if the mentor is already booked within 60 minutes of the requested time
        if (sessionRepository.hasMentorConflict(
                mentor,
                scheduledTime.minusMinutes(60),
                scheduledTime.plusMinutes(60))) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Mentor already has a booking that overlaps this time slot"));
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
        session.setLearner(learner);
        session.setSkill(skill);
        session.setMode(isOnline ? SessionMode.ONLINE : SessionMode.IN_PERSON);
        session.setStatus(SessionStatus.PENDING);
        session.setScheduledTime(scheduledTime);
        session.setDurationMinutes(60);
        session.setMeetingLink(meetingLink);
        session.setLocation(isOnline ? null : mentor.getLocation());

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
    }

    // Returns true if the full session window [scheduledTime, scheduledTime+durationMinutes)
    // fits inside one of the mentor's declared availability slots.
    // When the mentor has no availability records at all, booking is allowed (open availability).
    private boolean isWithinMentorAvailability(User mentor, LocalDateTime scheduledTime, int durationMinutes) {
        List<MentorAvailability> slots = availabilityRepository.findByMentor(mentor);
        if (slots.isEmpty()) {
            return true;
        }

        String requestedDay   = scheduledTime.getDayOfWeek().name();
        LocalTime sessionStart = scheduledTime.toLocalTime();
        LocalTime sessionEnd   = sessionStart.plusMinutes(durationMinutes);

        for (MentorAvailability slot : slots) {
            boolean dayMatches;
            if (slot.isRecurring()) {
                dayMatches = requestedDay.equalsIgnoreCase(slot.getDayOfWeek());
            } else {
                dayMatches = slot.getSpecificDate() != null
                    && slot.getSpecificDate().equals(scheduledTime.toLocalDate());
            }

            if (dayMatches) {
                LocalTime start = slot.getStartTime();
                LocalTime end   = slot.getEndTime();
                if (start != null && end != null
                        && !sessionStart.isBefore(start)  // session starts at or after slot start
                        && !sessionEnd.isAfter(end)) {    // session ends at or before slot end
                    return true;
                }
            }
        }
        return false;
    }
}