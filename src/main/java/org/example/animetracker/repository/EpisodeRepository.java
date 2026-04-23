package org.example.animetracker.repository;

import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.example.animetracker.model.Episode;
import org.example.animetracker.repository.projection.AnimeNextAiringDateProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {

  @Modifying
  @Transactional
  @Query("DELETE FROM Episode e WHERE e.season.id = :seasonId")
  void deleteAllBySeasonId(@Param("seasonId") Long seasonId);

  @Query("""
        SELECT e.season.id
        FROM Episode e
        GROUP BY e.season.id, e.number
        HAVING COUNT(e.id) > 1
        """)
  List<Long> findSeasonIdsWithDuplicates();

  @Query(value = """
        SELECT id FROM episodes
        WHERE season_id = :seasonId
          AND id NOT IN (
            SELECT MIN(id) FROM episodes
            WHERE season_id = :seasonId
            GROUP BY number
          )
        """, nativeQuery = true)
  List<Long> findDuplicateEpisodeIds(@Param("seasonId") Long seasonId);

  @Query("""
        SELECT s.anime.id AS animeId, MIN(e.releaseDate) AS nextAiringDate
        FROM Episode e
        JOIN e.season s
        WHERE s.anime.id IN :animeIds
          AND e.releaseDate IS NOT NULL
          AND e.releaseDate >= :today
        GROUP BY s.anime.id
        """)
  List<AnimeNextAiringDateProjection> findNextEpisodeDatesByAnimeIds(
      @Param("animeIds") Collection<Long> animeIds,
      @Param("today") LocalDate today);
}
