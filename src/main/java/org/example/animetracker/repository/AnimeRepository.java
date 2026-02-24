package org.example.animetracker.repository;

import java.util.List;
import java.util.Optional;
import org.example.animetracker.model.Anime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long> {

  List<Anime> findByStudioAndTitle(String studio, String title);

  List<Anime> findByStudio(String studio);

  List<Anime> findByTitle(String title);

  @Query("SELECT DISTINCT a FROM Anime a "
      + "LEFT JOIN FETCH a.genres "
      + "LEFT JOIN FETCH a.seasons s "
      + "LEFT JOIN FETCH s.episodes "
      + "WHERE a.id = :id")
  Optional<Anime> findByIdWithDetails(@Param("id") Long id);

}