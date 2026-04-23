package org.example.animetracker.mapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Episode;
import org.hibernate.Hibernate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnimeMapper {

  public static AnimeDto animeToDto(Anime anime) {
    if (anime == null) {
      return null;
    }
    AnimeDto dto =  new AnimeDto(
        anime.getId(),
        anime.getTitle(),
        anime.getNumOfReleasedSeasons(),
        anime.getStudio(),
        anime.getIsOngoing(),
        anime.getIsAnnounced(),
        anime.getPosterUrl()
    );

    LocalDate today = LocalDate.now();
    LocalDate nextAiringDate = null;
    if (Hibernate.isInitialized(anime.getSeasons())) {
      nextAiringDate = anime.getSeasons().stream()
          .filter(s -> Hibernate.isInitialized(s.getEpisodes()))
          .flatMap(s -> s.getEpisodes().stream())
          .map(Episode::getReleaseDate)
          .filter(d -> d != null && d.isAfter(today))
          .min(Comparator.naturalOrder())
          .orElse(null);
    }
    dto.setNextAiringDate(nextAiringDate);
    return dto;
  }

  public static AnimeDetailedDto animeToDetailedDto(Anime anime) {
    if (anime == null) {
      return null;
    }
    return new AnimeDetailedDto(
        anime.getId(),
        anime.getTitle(),
        anime.getNumOfReleasedSeasons(),
        anime.getStudio(),
        new ArrayList<>(new HashSet<>(anime.getSeasons())).stream()
            .map(SeasonMapper::seasonToDto)
            .toList(),
        anime.getIsOngoing(),
        anime.getIsAnnounced(),
        anime.getPosterUrl()
    );
  }
}