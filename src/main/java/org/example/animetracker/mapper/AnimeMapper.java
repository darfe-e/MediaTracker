package org.example.animetracker.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.SeasonDto;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Season;

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
        new ArrayList<>(new HashSet<>(anime.getSeasons())).stream()
            .map(SeasonMapper::seasonToDto)
            .toList(),
        anime.getIsOngoing()
    );
  }

  public static Anime deteiledDtoToAnime(AnimeDetailedDto dto) {
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
      Set<Season> seasons = new HashSet<>();
      for (SeasonDto seasonDto : dto.getSeasons()) {
        Season season = SeasonMapper.dtoToSeason(seasonDto);
        season.setAnime(anime);
        seasons.add(season);
      }
      anime.setSeasons(seasons);
    } else {
      anime.setSeasons(new HashSet<>());
    }
    return anime;
  }
}

