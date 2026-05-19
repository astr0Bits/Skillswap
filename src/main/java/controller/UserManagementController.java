package controller;

/*
 * Provides administrative endpoints for managing users. 
 * These are only accessible to users with the ADMIN role, 
 * enforced by the class‑level @PreAuthorize.
*/
import model.User;
import repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")//base path for admin endpoints
@CrossOrigin(origins = "*")//CORS
@PreAuthorize("hasRole('ADMIN')")//applies at class level: all methods in this controller require the ADMIN role.
public class UserManagementController {

    private final UserRepository userRepo;

    public UserManagementController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @GetMapping //Handles GET requests to /api/admin/users.
    public List<User> getAllUsers() {
    	//Returns a list of all users (JSON). Only accessible to admins.
        return userRepo.findAll();
    }

    @DeleteMapping("/{id}")//handles DELETE requests to /api/admin/users/{id}
    public void deleteUser(@PathVariable Long id) {
    	//Deletes a user by their database ID. Returns HTTP 200 with no body (void method).
        userRepo.deleteById(id);
    }
}