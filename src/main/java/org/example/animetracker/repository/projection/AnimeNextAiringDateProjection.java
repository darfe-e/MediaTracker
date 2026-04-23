package org.example.animetracker.repository.projection;

import java.time.LocalDate;

public interface AnimeNextAiringDateProjection {

  Long getAnimeId();

  LocalDate getNextAiringDate();
}
