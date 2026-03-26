package org.example.animetracker.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.AllArgsConstructor;
import org.example.animetracker.controller.api.ReviewControllerApi;
import org.example.animetracker.dto.ReviewCreateRequest;
import org.example.animetracker.dto.ReviewDto;
import org.example.animetracker.dto.ReviewUpdateRequest;
import org.example.animetracker.service.ReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/users/{userId}/review")
public class ReviewController implements ReviewControllerApi {

  private final ReviewService reviewService;

  @GetMapping("/{animeId}")
  public ResponseEntity<ReviewDto> getReview(
      @PathVariable Long userId, @PathVariable Long animeId) {
    ReviewDto reviewDto = reviewService.getReviewByUserAndAnime(userId, animeId);
    return ResponseEntity.ok(reviewDto);
  }

  @GetMapping
  public ResponseEntity<List<ReviewDto>> getAllReviews(@PathVariable Long userId) {
    List<ReviewDto> reviews = reviewService.getAllReviewsByUser(userId);
    return ResponseEntity.ok(reviews);
  }

  @PostMapping
  public ResponseEntity<ReviewDto> saveReview(
      @PathVariable Long userId,
      @Valid @RequestBody ReviewCreateRequest request) {
    ReviewDto saved = reviewService.saveReview(
        userId,
        request.getAnimeId(),
        request.getAssessment(),
        request.getText()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(saved);
  }

  @PutMapping("/{animeId}")
  public ResponseEntity<ReviewDto> updateReview(
      @PathVariable Long userId,
      @PathVariable Long animeId,
      @Valid @RequestBody ReviewUpdateRequest request) {
    ReviewDto updated = reviewService.updateReview(
        userId,
        animeId,
        request.getAssessment(),
        request.getText()
    );
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{animeId}")
  public ResponseEntity<Void> deleteReview(@PathVariable Long userId, @PathVariable Long animeId) {
    reviewService.deleteReview(userId, animeId);
    return ResponseEntity.noContent().build();
  }
}