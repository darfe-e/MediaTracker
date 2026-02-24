package org.example.animetracker.service;

import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.EpisodeDto;
import org.example.animetracker.dto.SeasonDto;
import org.example.animetracker.mapper.AnimeMapper;
import org.example.animetracker.mapper.EpisodeMapper;
import org.example.animetracker.mapper.SeasonMapper;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Episode;
import org.example.animetracker.model.Season;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.EpisodeRepository;
import org.example.animetracker.repository.SeasonRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class AnimeService {

  private final AnimeRepository animeRepository;
  private final SeasonRepository seasonRepository;
  private final EpisodeRepository episodeRepository;

  @Transactional(readOnly = true)
  public AnimeDetailedDto findById(Long id) {
    return animeRepository.findById(id)
        .map(AnimeMapper::animeToDetailedDto)
        .map(dto -> {
          dto.setSeasons(dto.getSeasons().stream()
              .filter(s -> !s.getEpisodes().isEmpty())
              .toList());
          return dto;
        })
        .orElse(null);
  }

  @Transactional(readOnly = true)
  public AnimeDetailedDto findByIdWithoutProblem(Long id) {
    return animeRepository.findByIdWithDetails(id)
        .map(anime -> {
          AnimeDetailedDto dto = AnimeMapper.animeToDetailedDto(anime);

          if (dto.getSeasons() != null) {
            List<SeasonDto> sortedSeasons = dto.getSeasons().stream()
                .sorted((s1, s2) ->
                    Integer.compare(s2.getEpisodes().size(), s1.getEpisodes().size()))
                .toList();
            dto.setSeasons(sortedSeasons);
          }

          return dto;
        })
        .orElse(null);
  }

  public List<AnimeDto> findByStudioAndName(String studio, String title) {
    if (studio != null && title != null) {
      return animeRepository.findByStudioAndTitle(studio, title).stream()
          .map(AnimeMapper::animeToDto)
          .toList();
    } else if (studio != null) {
      return animeRepository.findByStudio(studio).stream()
          .map(AnimeMapper::animeToDto)
          .toList();
    } else if (title != null) {
      return animeRepository.findByTitle(title).stream()
          .map(AnimeMapper::animeToDto)
          .toList();
    } else {
      return animeRepository.findAll().stream()
          .map(AnimeMapper::animeToDto)
          .toList();
    }
  }

  public List<AnimeDto> getAllSortedByPopularity() {
    return animeRepository.findAll().stream()
        .sorted(Comparator.comparingInt(Anime::getPopularityRank).reversed())
        .map(AnimeMapper::animeToDto)
        .toList();
  }

  public void createAnimeWithSeasonsWithoutTransaction(AnimeDetailedDto dto) {
    Anime anime = AnimeMapper.deteiledDtoToAnime(dto);
    anime = animeRepository.save(anime);

    for (SeasonDto seasonDto : dto.getSeasons()) {
      Season season = SeasonMapper.dtoToSeason(seasonDto);
      season.setAnime(anime);
      season = seasonRepository.save(season);

      for (EpisodeDto episodeDto : seasonDto.getEpisodes()) {
        Episode episode = EpisodeMapper.dtoToEpisode(episodeDto);
        episode.setSeason(season);
        episodeRepository.save(episode);
      }
    }

    throw new IllegalStateException("Ошибка после сохранения аниме и сезонов");
  }

  @Transactional
  public void createAnimeWithSeasonsWithTransaction(AnimeDetailedDto dto) {
    Anime anime = AnimeMapper.deteiledDtoToAnime(dto);
    anime = animeRepository.save(anime);

    for (SeasonDto seasonDto : dto.getSeasons()) {
      Season season = SeasonMapper.dtoToSeason(seasonDto);
      season.setAnime(anime);
      season = seasonRepository.save(season);

      for (EpisodeDto episodeDto : seasonDto.getEpisodes()) {
        Episode episode = EpisodeMapper.dtoToEpisode(episodeDto);
        episode.setSeason(season);
        episodeRepository.save(episode);
      }
    }

    throw new IllegalStateException("Ошибка после всех сохранений");
  }
}

