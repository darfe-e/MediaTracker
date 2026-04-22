package org.example.animetracker.service;

import lombok.RequiredArgsConstructor;
import org.example.animetracker.dto.EpisodeDto;
import org.example.animetracker.model.Season;
import org.example.animetracker.repository.SeasonRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeasonService {

  private final SeasonRepository seasonRepository;

  @Transactional(readOnly = true)
  public List<EpisodeDto> getEpisodesBySeasonId(Long seasonId) {
    Season season = seasonRepository.findById(seasonId)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Season not found: " + seasonId));

    return season.getEpisodes().stream()
        .sorted(Comparator.comparingInt(
            e -> e.getNumber() != null ? e.getNumber() : 0))
        .map(e -> new EpisodeDto(e.getTitle(), e.getNumber(), e.getReleaseDate()))
        .toList();
  }
}