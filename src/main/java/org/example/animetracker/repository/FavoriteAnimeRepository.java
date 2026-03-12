package org.example.animetracker.repository;

import java.util.List;
import java.util.Optional;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.FavoriteAnime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FavoriteAnimeRepository extends JpaRepository<FavoriteAnime, Long> {

  @EntityGraph(attributePaths = {"user", "anime"})
  List<Anime> findByUserId(Long userId);

  @EntityGraph(attributePaths = {"user", "anime"})
  Optional<FavoriteAnime> findByUserIdAndAnimeId(Long userId, Long animeId);

  @Query("SELECT f.anime FROM FavoriteAnime f LEFT JOIN f.review r "
      + "WHERE f.user.id = :userId ORDER BY r.assessment DESC NULLS LAST")
  Page<Anime> findAnimeByUserIdSortedByAssessment(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT f.anime FROM FavoriteAnime f "
      + "LEFT JOIN f.review r "
      + "WHERE f.user.id = :userId AND f.anime.isOngoing = true "
      + "ORDER BY r.assessment DESC NULLS LAST")
  Page<Anime> getOngoingSortedByAssessment(@Param("userId") Long userId, Pageable pageable);
}