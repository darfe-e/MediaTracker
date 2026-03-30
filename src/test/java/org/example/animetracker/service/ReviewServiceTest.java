package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.example.animetracker.dto.ReviewDto;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.FavoriteAnime;
import org.example.animetracker.model.Review;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.FavoriteAnimeRepository;
import org.example.animetracker.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock private ReviewRepository        reviewRepository;
  @Mock private FavoriteAnimeRepository favoriteAnimeRepository;

  @InjectMocks
  private ReviewService reviewService;

  @Test
  @DisplayName("getReviewByUserAndAnime — успех")
  void getReviewByUserAndAnime_success() {
    FavoriteAnime fav    = buildFavorite(1L);
    Review        review = buildReview(10L, fav, 8.5f, "Great anime");

    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.of(fav));
    when(reviewRepository.findByFavoriteId(1L))
        .thenReturn(Optional.of(review));

    ReviewDto result = reviewService.getReviewByUserAndAnime(1L, 2L);

    assertThat(result).isNotNull();
    assertThat(result.getAssessment()).isEqualTo(8.5f);
  }

  @Test
  @DisplayName("getReviewByUserAndAnime — избранное не найдено → 404")
  void getReviewByUserAndAnime_favoriteNotFound_throws404() {
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.getReviewByUserAndAnime(1L, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Favorite not found for user 1");
  }

  @Test
  @DisplayName("getReviewByUserAndAnime — отзыв не найден → 404")
  void getReviewByUserAndAnime_reviewNotFound_throws404() {
    FavoriteAnime fav = buildFavorite(1L);

    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.of(fav));
    when(reviewRepository.findByFavoriteId(1L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.getReviewByUserAndAnime(1L, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Review not found for user 1");
  }

  @Test
  @DisplayName("getAllReviewsByUser — возвращает все отзывы пользователя")
  void getAllReviewsByUser_returnsList() {
    FavoriteAnime fav = buildFavorite(1L);
    List<Review> reviews = List.of(
        buildReview(1L, fav, 9.0f, "Excellent"),
        buildReview(2L, fav, 7.5f, "Good")
    );
    when(reviewRepository.findByUserId(5L)).thenReturn(reviews);

    List<ReviewDto> result = reviewService.getAllReviewsByUser(5L);

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("getAllReviewsByUser — у пользователя нет отзывов → пустой список")
  void getAllReviewsByUser_empty_returnsEmptyList() {
    when(reviewRepository.findByUserId(5L)).thenReturn(List.of());

    List<ReviewDto> result = reviewService.getAllReviewsByUser(5L);

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("saveReview — успешное создание отзыва")
  void saveReview_success() {
    FavoriteAnime fav    = buildFavorite(1L);
    Review        saved  = buildReview(10L, fav, 9.0f, "Amazing!");

    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.of(fav));
    when(reviewRepository.findByFavoriteId(1L))
        .thenReturn(Optional.empty());
    when(reviewRepository.save(any(Review.class))).thenReturn(saved);

    ReviewDto result = reviewService.saveReview(1L, 2L, 9.0f, "Amazing!");

    assertThat(result.getAssessment()).isEqualTo(9.0f);
    assertThat(result.getText()).isEqualTo("Amazing!");
    verify(reviewRepository).save(any(Review.class));
  }

  @Test
  @DisplayName("saveReview — избранное не найдено → 404")
  void saveReview_favoriteNotFound_throws404() {
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.saveReview(1L, 2L, 8f, "text"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Favorite not found for user 1");
  }

  @Test
  @DisplayName("saveReview — отзыв уже существует → 409 Conflict")
  void saveReview_duplicate_throws409() {
    FavoriteAnime fav = buildFavorite(1L);
    Review existing   = buildReview(5L, fav, 7f, "old");

    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.of(fav));
    when(reviewRepository.findByFavoriteId(1L))
        .thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> reviewService.saveReview(1L, 2L, 8f, "new"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT))
        .hasMessageContaining("Review already exists");
  }

  @Test
  @DisplayName("updateReview — успешное обновление отзыва")
  void updateReview_success() {
    FavoriteAnime fav    = buildFavorite(1L);
    Review        review = buildReview(5L, fav, 7.0f, "old text");
    Review        updated = buildReview(5L, fav, 9.5f, "new text");

    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.of(fav));
    when(reviewRepository.findByFavoriteId(1L))
        .thenReturn(Optional.of(review));
    when(reviewRepository.save(any(Review.class))).thenReturn(updated);

    ReviewDto result = reviewService.updateReview(1L, 2L, 9.5f, "new text");

    assertThat(result.getAssessment()).isEqualTo(9.5f);
    assertThat(result.getText()).isEqualTo("new text");
    verify(reviewRepository).save(review);
  }

  @Test
  @DisplayName("updateReview — избранное не найдено → 404")
  void updateReview_favoriteNotFound_throws404() {
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.updateReview(1L, 2L, 9f, "text"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Favorite not found for user 1");
  }

  @Test
  @DisplayName("updateReview — отзыв не найден → 404")
  void updateReview_reviewNotFound_throws404() {
    FavoriteAnime fav = buildFavorite(1L);

    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.of(fav));
    when(reviewRepository.findByFavoriteId(1L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.updateReview(1L, 2L, 9f, "text"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Review not found for user 1");
  }

  @Test
  @DisplayName("deleteReview — успешное удаление")
  void deleteReview_success() {
    FavoriteAnime fav    = buildFavorite(1L);
    Review        review = buildReview(5L, fav, 7f, "text");

    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.of(fav));
    when(reviewRepository.findByFavoriteId(1L))
        .thenReturn(Optional.of(review));

    reviewService.deleteReview(1L, 2L);

    verify(reviewRepository).delete(review);
  }

  @Test
  @DisplayName("deleteReview — избранное не найдено → 404")
  void deleteReview_favoriteNotFound_throws404() {
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.deleteReview(1L, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Favorite not found for user 1");
  }

  @Test
  @DisplayName("deleteReview — отзыв не найден → 404")
  void deleteReview_reviewNotFound_throws404() {
    FavoriteAnime fav = buildFavorite(1L);

    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L))
        .thenReturn(Optional.of(fav));
    when(reviewRepository.findByFavoriteId(1L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.deleteReview(1L, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Review not found for user 1");
  }

  private FavoriteAnime buildFavorite(Long id) {
    User  user  = new User();
    user.setId(1L);
    Anime anime = new Anime();
    anime.setId(2L);
    FavoriteAnime fav = new FavoriteAnime(user, anime);
    fav.setId(id);
    return fav;
  }

  private Review buildReview(Long id, FavoriteAnime fav, float assessment, String text) {
    Review r = new Review();
    r.setId(id);
    r.setFavorite(fav);
    r.setAssessment(assessment);
    r.setText(text);
    return r;
  }
}