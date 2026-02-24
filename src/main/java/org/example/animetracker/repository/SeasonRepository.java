package org.example.animetracker.repository;

import org.example.animetracker.model.Season;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonRepository extends JpaRepository<Season, Long> {

}