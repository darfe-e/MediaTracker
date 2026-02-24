package org.example.animetracker.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.EpisodeDto;
import org.example.animetracker.model.Episode;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EpisodeMapper {
  public static EpisodeDto episodeToDto(Episode episode) {
    if (episode == null) {
      return null;
    }
    return new EpisodeDto(episode.getTitle(), episode.getNumber(),
        episode.getReleaseDate(), episode.getIsReleased());
  }

  public static Episode dtoToEpisode(EpisodeDto dto) {
    if (dto == null) {
      return null;
    }
    Episode episode = new Episode();
    episode.setTitle(dto.getTitle());
    episode.setNumber(dto.getNumber());
    episode.setReleaseDate(dto.getReleaseDate());
    episode.setIsReleased(dto.getIsReleased());
    return episode;
  }
}