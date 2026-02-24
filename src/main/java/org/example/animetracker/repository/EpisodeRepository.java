package org.example.animetracker.repository;

import org.example.animetracker.model.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {


}