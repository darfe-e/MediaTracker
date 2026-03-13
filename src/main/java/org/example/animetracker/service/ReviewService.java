package org.example.animetracker.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.dto.ReviewDto;
import org.example.animetracker.mapper.ReviewMapper;
import org.example.animetracker.model.FavoriteAnime;
import org.example.animetracker.model.Review;
import org.example.animetracker.repository.FavoriteAnimeRepository;
import org.example.animetracker.repository.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@AllArgsConstructor
@Service
public class ReviewService {

  private static final String FAVORITE_NOT_FOUND_PREFIX = "Favorite not found for user ";
  private static final String REVIEW_NOT_FOUND_PREFIX = "Review not found for user ";
  private static final String AND_ANIME_SUFFIX = " and anime ";
  private static final String REVIEW_ALREADY_EXISTS = "Review already exists for this favorite";

  private final ReviewRepository reviewRepository;
  private final FavoriteAnimeRepository favoriteAnimeRepository;

  private FavoriteAnime getFavoriteOrThrow(Long userId, Long animeId) {
    return favoriteAnimeRepository.findByUserIdAndAnimeId(userId, animeId)
        .orElseThrow(() -> {
          log.error("Favorite not found for user {} and anime {}", userId, animeId);
          return new ResponseStatusException(HttpStatus.NOT_FOUND,
              FAVORITE_NOT_FOUND_PREFIX + userId + AND_ANIME_SUFFIX + animeId);
        });
  }

  private Review getReviewOrThrow(FavoriteAnime favorite, Long userId, Long animeId) {
    return reviewRepository.findByFavoriteId(favorite.getId())
        .orElseThrow(() -> {
          log.error("Review not found for user {} and anime {}", userId, animeId);
          return new ResponseStatusException(HttpStatus.NOT_FOUND,
              REVIEW_NOT_FOUND_PREFIX + userId + AND_ANIME_SUFFIX + animeId);
        });
  }

  @Transactional(readOnly = true)
  public ReviewDto getReviewByUserAndAnime(Long userId, Long animeId) {
    FavoriteAnime favorite = getFavoriteOrThrow(userId, animeId);
    Review review = getReviewOrThrow(favorite, userId, animeId);
    return ReviewMapper.reviewToDto(review);
  }

  @Transactional(readOnly = true)
  public List<ReviewDto> getAllReviewsByUser(Long userId) {
    List<ReviewDto> reviews = reviewRepository.findByUserId(userId).stream()
        .map(ReviewMapper::reviewToDto)
        .toList();
    log.debug("Found {} reviews for user {}", reviews.size(), userId);
    return reviews;
  }

  @Transactional
  public ReviewDto saveReview(Long userId, Long animeId, Float assessment, String text) {
    FavoriteAnime favorite = getFavoriteOrThrow(userId, animeId);

    if (reviewRepository.findByFavoriteId(favorite.getId()).isPresent()) {
      log.warn("Attempt to save duplicate review for favorite id {}", favorite.getId());
      throw new ResponseStatusException(HttpStatus.CONFLICT, REVIEW_ALREADY_EXISTS);
    }

    Review review = new Review();
    review.setAssessment(assessment);
    review.setText(text);
    review.setFavorite(favorite);
    Review saved = reviewRepository.save(review);
    log.info("Review saved with id {} for favorite id {}", saved.getId(), favorite.getId());
    return ReviewMapper.reviewToDto(saved);
  }

  @Transactional
  public ReviewDto updateReview(Long userId, Long animeId, Float assessment, String text) {
    FavoriteAnime favorite = getFavoriteOrThrow(userId, animeId);
    Review review = getReviewOrThrow(favorite, userId, animeId);

    review.setAssessment(assessment);
    review.setText(text);
    Review updated = reviewRepository.save(review);
    log.info("Review updated for favorite id {}", favorite.getId());
    return ReviewMapper.reviewToDto(updated);
  }

  @Transactional
  public void deleteReview(Long userId, Long animeId) {
    FavoriteAnime favorite = getFavoriteOrThrow(userId, animeId);
    Review review = getReviewOrThrow(favorite, userId, animeId);
    reviewRepository.delete(review);
    log.info("Review deleted for favorite id {}", favorite.getId());
  }
}