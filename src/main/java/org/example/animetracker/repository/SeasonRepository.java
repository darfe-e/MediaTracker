package org.example.animetracker.repository;

import java.util.Optional;
import org.example.animetracker.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

  Optional<Season> findByExternalId(Long externalId);
}