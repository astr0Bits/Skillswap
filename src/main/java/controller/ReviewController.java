package controller;

import model.Review;
import repository.ReviewRepository;
import model.User;
import repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import dto.ReviewDTO;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public ReviewController(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

 // controller/ReviewController.java
    @GetMapping("/me")
    public ResponseEntity<List<ReviewDTO>> getMyReviews(Authentication auth) {
        User user = getUserFromAuth(auth);
        List<Review> reviews = reviewRepository.findByRevieweeOrderByCreatedAtDesc(user);
        List<ReviewDTO> dtos = reviews.stream().map(this::toDTO).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private ReviewDTO toDTO(Review r) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(r.getId());
        dto.setReviewerName(r.getReviewer().getName());
        dto.setRating(r.getRating());
        dto.setComment(r.getComment());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }

    private User getUserFromAuth(Authentication auth) {
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}