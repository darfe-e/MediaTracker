package org.example.animetracker.mapper;

import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.model.Anime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnimeMapper {
  public static AnimeDto animeToDto(Anime anime) {
    if (anime == null) {
      return null;
    }
    return new AnimeDto(anime.getId(), anime.getTitle(), anime.getNumOfReleasedSeasons(),
        anime.getStudio(), anime.getIsOngoing());
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
        anime.getSeasons() == null ? new ArrayList<>() :
            anime.getSeasons().stream()
                .map(SeasonMapper::seasonToDto)
                .toList(),
        anime.getIsOngoing()
    );
  }

  public static Anime dtoToAnime(AnimeDto dto) {
    if (dto == null) {
      return null;
    }
    Anime anime = new Anime();
    anime.setId(dto.getId());
    anime.setTitle(dto.getTitle());
    anime.setNumOfReleasedSeasons(dto.getNumOfReleasedSeasons());
    anime.setStudio(dto.getStudio());
    anime.setIsOngoing(dto.getIsOngoing());
    anime.setPopularityRank(null);
    anime.setSeasons(new ArrayList<>());
    return anime;
  }

  public static Anime dtoToAnime(AnimeDetailedDto dto) {
    if (dto == null) {
      return null;
    }
    Anime anime = new Anime();
    anime.setId(dto.getId());
    anime.setTitle(dto.getTitle());
    anime.setNumOfReleasedSeasons(dto.getNumOfReleasedSeasons());
    anime.setStudio(dto.getStudio());
    anime.setIsOngoing(dto.getIsOngoing());
    anime.setPopularityRank(null);
    if (dto.getSeasons() != null) {
      anime.setSeasons(
          dto.getSeasons().stream()
              .map(SeasonMapper::dtoToSeason)
              .toList()
      );
    } else {
      anime.setSeasons(new ArrayList<>());
    }
    return anime;
  }
}