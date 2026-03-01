package org.example.animetracker.service;

import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.example.animetracker.dto.ReviewDto;
import org.example.animetracker.mapper.ReviewMapper;
import org.example.animetracker.model.FavoriteAnime;
import org.example.animetracker.model.Review;
import org.example.animetracker.repository.FavoriteAnimeRepository;
import org.example.animetracker.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final FavoriteAnimeRepository favoriteAnimeRepository;

  @Transactional(readOnly = true)
  public Optional<ReviewDto> getReviewByUserAndAnime(Long userId, Long animeId) {
    return reviewRepository.findByUserAndAnime(userId, animeId)
        .map(ReviewMapper::reviewToDto);
  }

  @Transactional(readOnly = true)
  public List<ReviewDto> getAllReviewsByUser(Long userId) {
    return reviewRepository.findByFavorite_UserId(userId).stream()
        .map(ReviewMapper::reviewToDto)
        .toList();
  }

  @Transactional
  public ReviewDto saveReview(ReviewDto reviewDto) {
    Long userId = reviewDto.getFavorite().getUser().getId();
    Long animeId = reviewDto.getFavorite().getAnime().getId();

    FavoriteAnime favorite = favoriteAnimeRepository.findByUserIdAndAnimeId(
        userId, animeId).orElse(null);

    if (favorite == null) {
      return null;
    }

    if (reviewRepository.findByFavoriteId(favorite.getId()).isPresent()) {
      return null;
    }

    Review review = new Review();
    review.setAssessment(reviewDto.getAssessment());
    review.setText(reviewDto.getText());
    review.setFavorite(favorite);

    reviewRepository.save(review);
    return ReviewMapper.reviewToDto(review);
  }

  @Transactional
  public ReviewDto updateReview(ReviewDto reviewDto) {
    Long userId = reviewDto.getFavorite().getUser().getId();
    Long animeId = reviewDto.getFavorite().getAnime().getId();

    FavoriteAnime favorite = favoriteAnimeRepository.findByUserIdAndAnimeId(userId, animeId)
        .orElse(null);
    if (favorite == null) {
      return null;
    }

    Review review = reviewRepository.findByFavoriteId(favorite.getId())
        .orElse(null);
    if (review == null) {
      return null;
    }

    review.setAssessment(reviewDto.getAssessment());
    review.setText(reviewDto.getText());

    reviewRepository.save(review);
    return ReviewMapper.reviewToDto(review);
  }

  @Transactional
  public boolean deleteReview(Long userId, Long animeId) {
    return reviewRepository.findByUserAndAnime(userId, animeId)
        .map(review -> {
          reviewRepository.delete(review);
          return true;
        })
        .orElse(false);
  }
}