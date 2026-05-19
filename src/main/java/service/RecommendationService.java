package service;

import dto.RecommendationDTO;
import model.User;
import model.UserSkill;
import repository.SkillRepository;
import repository.UserSkillRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final UserSkillRepository userSkillRepository;
    private final SkillRepository skillRepository;

    public RecommendationService(UserSkillRepository userSkillRepository,
                                 SkillRepository skillRepository) {
        this.userSkillRepository = userSkillRepository;
        this.skillRepository = skillRepository;
    }

    public List<RecommendationDTO> getRecommendations(User user, String learningGoals) {

        List<RecommendationDTO> recs = new ArrayList<>();

        // get user's LEARN skills (already interested)
        List<String> learningSkillNames = userSkillRepository
                .findByUserIdAndType(user.getId(), UserSkill.SkillType.LEARN)
                .stream()
                .map(us -> us.getSkill().getName())
                .collect(Collectors.toList());

        // helper method to create DTO
        java.util.function.BiFunction<String, Integer, RecommendationDTO> createRec =
                (name, match) -> {
                    RecommendationDTO dto = new RecommendationDTO();
                    dto.setSkillName(name);
                    dto.setMatchPercent(match);
                    return dto;
                };

        // fallback recommendations based on goals or popular skills
        if (learningGoals != null && learningGoals.toLowerCase().contains("data")) {

            RecommendationDTO dto1 = createRec.apply("Data Visualization", 92);
            dto1.setDescription("Tableau & Power BI");
            recs.add(dto1);

            RecommendationDTO dto2 = createRec.apply("SQL & Database Querying", 88);
            dto2.setDescription("Advanced queries");
            recs.add(dto2);

        } else if (learningGoals != null && learningGoals.toLowerCase().contains("javascript")) {

            RecommendationDTO dto1 = createRec.apply("JavaScript Development", 94);
            dto1.setDescription("Modern JS frameworks");
            recs.add(dto1);

            RecommendationDTO dto2 = createRec.apply("React / Vue / Angular", 89);
            dto2.setDescription("Frontend mastery");
            recs.add(dto2);

        } else {

            RecommendationDTO dto1 = createRec.apply("Python Programming", 85);
            dto1.setDescription("Automation & scripting");
            recs.add(dto1);

            RecommendationDTO dto2 = createRec.apply("Public Speaking", 78);
            dto2.setDescription("Presentation skills");
            recs.add(dto2);
        }

        // add one more from user's learning skills if any
        if (!learningSkillNames.isEmpty()) {
            RecommendationDTO dto = createRec.apply(learningSkillNames.get(0) + " (Advanced)", 90);
            dto.setDescription("Deepen your knowledge");
            recs.add(dto);
        }

        return recs.stream().limit(3).collect(Collectors.toList());
    }
}