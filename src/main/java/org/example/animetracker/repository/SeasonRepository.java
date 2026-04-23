package org.example.animetracker.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.example.animetracker.model.Season;
import org.example.animetracker.repository.projection.AnimeNextAiringDateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonRepository extends JpaRepository<Season, Long> {

  Optional<Season> findByExternalId(Long externalId);

  @Query("""
        SELECT s.anime.id AS animeId, MIN(s.releaseDate) AS nextAiringDate
        FROM Season s
        WHERE s.anime.id IN :animeIds
          AND s.releaseDate IS NOT NULL
          AND s.releaseDate >= :today
          AND COALESCE(s.isReleased, false) = false
        GROUP BY s.anime.id
        """)
  List<AnimeNextAiringDateProjection> findUpcomingSeasonDatesByAnimeIds(
      @Param("animeIds") Collection<Long> animeIds,
      @Param("today") LocalDate today);
}
