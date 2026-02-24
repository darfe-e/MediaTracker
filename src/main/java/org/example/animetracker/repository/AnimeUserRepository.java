package org.example.animetracker.repository;

import java.util.List;
import java.util.Optional;
import org.example.animetracker.model.AnimeUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimeUserRepository extends JpaRepository<AnimeUser, Long> {

  @EntityGraph(attributePaths = {"user", "anime"})
  List<AnimeUser> findByUserId(Long userId);

  @EntityGraph(attributePaths = {"user", "anime"})
  Optional<AnimeUser> findByUserIdAndAnimeId(Long userId, Long animeId);

}