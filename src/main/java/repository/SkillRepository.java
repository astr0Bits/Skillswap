// repository/SkillRepository.java
package repository;

import model.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByNameContainingIgnoreCase(String name);
    Optional<Skill> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
}