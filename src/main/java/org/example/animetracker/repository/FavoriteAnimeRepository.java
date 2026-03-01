package org.example.animetracker.repository;

import java.util.List;
import java.util.Optional;
import org.example.animetracker.model.FavoriteAnime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteAnimeRepository extends JpaRepository<FavoriteAnime, Long> {

  @EntityGraph(attributePaths = {"user", "anime"})
  List<FavoriteAnime> findByUserId(Long userId);

  @EntityGraph(attributePaths = {"user", "anime"})
  Optional<FavoriteAnime> findByUserIdAndAnimeId(Long userId, Long animeId);

}