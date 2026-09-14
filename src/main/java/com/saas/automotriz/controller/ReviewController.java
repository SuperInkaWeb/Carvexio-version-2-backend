package com.saas.automotriz.controller;

import com.saas.automotriz.dto.ReviewDTO;
import com.saas.automotriz.request.ReviewRequest;
import com.saas.automotriz.model.Business;
import com.saas.automotriz.model.Review;
import com.saas.automotriz.model.User;
import com.saas.automotriz.repository.BusinessRepository;
import com.saas.automotriz.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewRepository reviewRepository;
    private final BusinessRepository businessRepository;

    // público - cualquiera puede ver reseñas de un negocio
    @GetMapping("/business/{businessId}")
    public ResponseEntity<List<ReviewDTO>> getReviews(@PathVariable Long businessId) {
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));
        List<ReviewDTO> reviews = reviewRepository.findByBusiness(business)
                .stream().map(this::toDTO).toList();
        return ResponseEntity.ok(reviews);
    }

    // solo CLIENTE puede dejar reseña
    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@AuthenticationPrincipal User user,
                                                  @RequestBody ReviewRequest request) {
        // validar rating
        if (request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest().build();
        }

        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(() -> new RuntimeException("Negocio no encontrado"));

        // un cliente solo puede dejar una reseña por negocio
        if (reviewRepository.existsByClientAndBusiness(user, business)) {
            return ResponseEntity.status(409).build(); // 409 Conflict
        }

        Review review = new Review();
        review.setClient(user);
        review.setBusiness(business);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        return ResponseEntity.ok(toDTO(reviewRepository.save(review)));
    }

    private ReviewDTO toDTO(Review r) {
        ReviewDTO dto = new ReviewDTO();
        dto.setId(r.getId());
        dto.setRating(r.getRating());
        dto.setComment(r.getComment());
        dto.setClientName(r.getClient().getName());
        dto.setCreatedAt(r.getCreatedAt());
        return dto;
    }
}