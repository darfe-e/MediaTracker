package org.example.animetracker.repository;

import java.util.List;
import java.util.Optional;
import org.example.animetracker.model.Anime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long> {

  List<Anime> findByStudioAndTitle(String studio, String title);

  List<Anime> findByStudio(String studio);

  List<Anime> findByTitle(String title);

  @Query ("SELECT a FROM Anime a ORDER BY a.popularityRank DESC")
  Page<Anime> findAllSorted(Pageable pageable);

  @Query("SELECT DISTINCT a FROM Anime a "
      + "LEFT JOIN FETCH a.genres "
      + "LEFT JOIN FETCH a.seasons s "
      + "LEFT JOIN FETCH s.episodes "
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

}