package org.example.animetracker.mapper;

import java.util.ArrayList;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.SeasonDto;
import org.example.animetracker.model.Season;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SeasonMapper {
  public static SeasonDto seasonToDto(Season season) {
    if (season == null) {
      return null;
    }
    return new SeasonDto(
        season.getReleaseDate(),
        season.getIsReleased(),
        season.getEpisodes() == null ? new ArrayList<>() :
            season.getEpisodes().stream()
                .map(EpisodeMapper::episodeToDto)
                .toList()
    );
  }
}