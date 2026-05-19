package controller;

import dto.MentorBrowseDTO;
import service.BrowseService;
import service.UserSkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/browse")
@CrossOrigin(origins = "*")
public class BrowseController {

    private final BrowseService browseService;
    private final UserSkillService userSkillService;

    public BrowseController(BrowseService browseService, UserSkillService userSkillService) {
        this.browseService = browseService;
        this.userSkillService = userSkillService;
    }

    @GetMapping("/mentors")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<MentorBrowseDTO>> browseMentors(
            @RequestParam(required = false) Map<String, String> filters) {
        List<MentorBrowseDTO> mentors = browseService.findMentors(filters);
        return ResponseEntity.ok(mentors);
    }

    @GetMapping("/skills")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> browseSkills(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(userSkillService.searchMentorsBySkill(q));
    }
}