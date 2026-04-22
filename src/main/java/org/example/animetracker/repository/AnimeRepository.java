package org.example.animetracker.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.example.animetracker.model.Anime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long>,
    JpaSpecificationExecutor<Anime> {

  List<Anime> findByStudioAndTitle(String studio, String title);

  List<Anime> findByStudio(String studio);

  List<Anime> findByTitle(String title);

  @Query ("SELECT a FROM Anime a ORDER BY a.popularityRank DESC")
  Page<Anime> findAllSorted(Pageable pageable);

  @Query("SELECT DISTINCT a FROM Anime a "
      + "LEFT JOIN FETCH a.genres "
      + "LEFT JOIN FETCH a.seasons s "
      + "WHERE a.id = :id")
  Optional<Anime> findByIdWithDetails(@Param("id") Long id);

  Optional<Anime> findByExternalId(Long externalId);

  @Query("SELECT DISTINCT a FROM Anime a "
      + "JOIN a.genres g WHERE g.name = :genre "
      + "AND a.numOfReleasedSeasons >= :minSeasons")
  Page<Anime> findByGenreAndMinSeasons(@Param("genre") String genre,
                                       @Param("minSeasons") int minSeasons,
                                       Pageable pageable);

  @Query(value = "SELECT DISTINCT a.* FROM animes a "
      + "JOIN anime_genre ag ON a.id = ag.anime_id "
      + "JOIN genres g ON ag.genre_id = g.id "
      + "WHERE g.name = :genre AND a.num_of_released_seasons >= :minSeasons",
      countQuery = "SELECT COUNT(DISTINCT a.id) FROM animes a "
          + "JOIN anime_genre ag ON a.id = ag.anime_id "
          + "JOIN genres g ON ag.genre_id = g.id "
          + "WHERE g.name = :genre AND a.num_of_released_seasons >= :minSeasons",
      nativeQuery = true)
  Page<Anime> findByGenreAndMinSeasonsNative(@Param("genre") String genre,
                                             @Param("minSeasons") int minSeasons,
                                             Pageable pageable);

  @Query("SELECT a.externalId FROM Anime a WHERE a.externalId IS NOT NULL")
  List<Long> findAllExternalIds();

  Optional<Anime> findByTitleIgnoreCase(String title);

  @Query("SELECT a.externalId FROM Anime a "
      + "WHERE a.isOngoing = :ongoing AND a.externalId IS NOT NULL")
  List<Long> findExternalIdsByIsOngoing(@Param("ongoing") boolean ongoing);

  @Query("""
            SELECT a.externalId FROM Anime a
            WHERE a.isOngoing = false
              AND a.externalId IS NOT NULL
              AND (a.lastUpdated IS NULL OR a.lastUpdated < :threshold)
            """)
  List<Long> findExternalIdsByIsOngoingFalseAndLastUpdatedBefore(
      @Param("threshold") LocalDateTime threshold);

  Page<Anime> findAll(Specification<Anime> spec, Pageable pageable);

  List<Anime> findByStudioContainingIgnoreCase(String studio);

  List<Long> findExternalIdsByIsAnnounced(boolean isAnnounced);

  List<Anime> findTop10ByTitleContainingIgnoreCaseOrderByPopularityRankDesc(String title);

  List<Anime> findTop30ByIsOngoingFalseAndIdGreaterThanOrderByIdAsc(Long id);
}