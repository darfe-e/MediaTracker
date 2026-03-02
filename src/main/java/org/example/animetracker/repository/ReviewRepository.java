package org.example.animetracker.repository;

import java.util.List;
import java.util.Optional;
import org.example.animetracker.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {
  Optional<Review> findByFavoriteId(Long favoriteId);

  @Query("SELECT r FROM Review r WHERE r.favorite.user.id = :userId")
  List<Review> findByUserId(@Param("userId") Long userId);

  @Query("SELECT r FROM Review r WHERE r.favorite.user.id = :userId "
      + "AND r.favorite.anime.id = :animeId")
  Optional<Review> findByUserAndAnime(@Param("userId") Long userId, @Param("animeId") Long animeId);
}
