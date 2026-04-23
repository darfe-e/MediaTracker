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
import org.example.animetracker.model.Season;
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

    dto.setNextAiringDate(resolveNextAiringDate(anime));
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

  private static LocalDate resolveNextAiringDate(Anime anime) {
    if (!Hibernate.isInitialized(anime.getSeasons())) {
      return null;
    }

    LocalDate today = LocalDate.now();
    return anime.getSeasons().stream()
        .map(season -> getNearestUpcomingDate(season, today))
        .filter(date -> date != null)
        .min(Comparator.naturalOrder())
        .orElse(null);
  }

  private static LocalDate getNearestUpcomingDate(Season season, LocalDate today) {
    LocalDate nextEpisodeDate = null;
    if (Hibernate.isInitialized(season.getEpisodes())) {
      nextEpisodeDate = season.getEpisodes().stream()
          .map(Episode::getReleaseDate)
          .filter(date -> date != null && !date.isBefore(today))
          .min(Comparator.naturalOrder())
          .orElse(null);
    }

    LocalDate seasonReleaseDate = season.getReleaseDate();
    if (seasonReleaseDate == null
        || seasonReleaseDate.isBefore(today)
        || Boolean.TRUE.equals(season.getIsReleased())) {
      return nextEpisodeDate;
    }

    if (nextEpisodeDate == null || seasonReleaseDate.isBefore(nextEpisodeDate)) {
      return seasonReleaseDate;
    }
    return nextEpisodeDate;
  }
}
