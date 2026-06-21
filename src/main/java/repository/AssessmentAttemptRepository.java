package repository;

import model.Assessment;
import model.AssessmentAttempt;
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, Long> {
    long countByUserAndAssessmentAndPassed(User user, Assessment assessment, boolean passed);
    List<AssessmentAttempt> findByUserOrderByCompletedAtDesc(User user);
}
