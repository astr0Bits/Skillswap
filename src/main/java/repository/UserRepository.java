package repository;
/*Provides data access for the User entity. 
 * It’s used by UserService and UserController 
 * (and likely by the authentication service) 
 * to fetch, save, and delete users. The existence methods are used 
 * for validation (e.g., checking if email is already taken).
*/
import model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    void deleteByEmail(String email);
}