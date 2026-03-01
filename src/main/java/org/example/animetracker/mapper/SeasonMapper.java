package org.example.animetracker.mapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.EpisodeDto;
import org.example.animetracker.dto.SeasonDto;
import org.example.animetracker.model.Episode;
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

  public static Season dtoToSeason(SeasonDto dto) {
    if (dto == null) {
      return null;
    }
    Season season = new Season();
    season.setReleaseDate(dto.getReleaseDate());
    season.setIsReleased(dto.getIsReleased());
    if (dto.getEpisodes() != null) {
      season.setTotalEpisodes(dto.getEpisodes().size());
      Set<Episode> episodes = new HashSet<>();
      for (EpisodeDto episodeDto : dto.getEpisodes()) {
        Episode episode = EpisodeMapper.dtoToEpisode(episodeDto);
        episode.setSeason(season);
        episodes.add(episode);
      }
      season.setEpisodes(episodes);
    } else {
      season.setTotalEpisodes(0);
      season.setEpisodes(new HashSet<>());
    }
    return season;
  }
}