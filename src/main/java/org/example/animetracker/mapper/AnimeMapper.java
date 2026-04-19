package org.example.animetracker.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.model.Anime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnimeMapper {

  public static AnimeDto animeToDto(Anime anime) {
    if (anime == null) return null;
    return new AnimeDto(
        anime.getId(),
        anime.getTitle(),
        anime.getNumOfReleasedSeasons(),
        anime.getStudio(),
        anime.getIsOngoing(),
        anime.getIsAnnounced(),
        anime.getPosterUrl()
    );
  }

  public static AnimeDetailedDto animeToDetailedDto(Anime anime) {
    if (anime == null) return null;
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