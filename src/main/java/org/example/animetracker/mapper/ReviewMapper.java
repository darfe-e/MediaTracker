package org.example.animetracker.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.ReviewDto;
import org.example.animetracker.model.Review;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReviewMapper {
  public static ReviewDto reviewToDto(Review review) {
    if (review == null) {
      return null;
    }
    return new ReviewDto(
        review.getId(),
        FavoriteAnimeMapper.favoriteAnimeToDto(review.getFavorite()),
        review.getAssessment(),
        review.getText()
    );
  }
}
