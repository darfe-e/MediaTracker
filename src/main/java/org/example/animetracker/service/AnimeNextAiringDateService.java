package org.example.animetracker.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.repository.EpisodeRepository;
import org.example.animetracker.repository.SeasonRepository;
import org.example.animetracker.repository.projection.AnimeNextAiringDateProjection;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnimeNextAiringDateService {

  private final SeasonRepository seasonRepository;
  private final EpisodeRepository episodeRepository;

  @Transactional(readOnly = true)
  public Page<AnimeDto> enrich(Page<AnimeDto> animePage) {
    enrich(animePage.getContent());
    return animePage;
  }

  @Transactional(readOnly = true)
  public AnimeDto enrich(AnimeDto animeDto) {
    if (animeDto == null) {
      return null;
    }
    enrich(List.of(animeDto));
    return animeDto;
  }

  @Transactional(readOnly = true)
  public List<AnimeDto> enrich(List<AnimeDto> animeDtos) {
    if (animeDtos == null || animeDtos.isEmpty()) {
      return animeDtos;
    }

    List<Long> animeIds = animeDtos.stream()
        .filter(dto -> Boolean.TRUE.equals(dto.getIsOngoing())
            || Boolean.TRUE.equals(dto.getIsAnnounced()))
        .map(AnimeDto::getId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (animeIds.isEmpty()) {
      return animeDtos;
    }

    LocalDate today = LocalDate.now();
    Map<Long, LocalDate> nextDatesByAnimeId = new HashMap<>();

    seasonRepository.findUpcomingSeasonDatesByAnimeIds(animeIds, today)
        .forEach(result -> mergeNextDate(nextDatesByAnimeId, result));
    episodeRepository.findNextEpisodeDatesByAnimeIds(animeIds, today)
        .forEach(result -> mergeNextDate(nextDatesByAnimeId, result));

    animeDtos.forEach(dto -> mergeIntoDto(dto, nextDatesByAnimeId.get(dto.getId())));
    return animeDtos;
  }

  private void mergeNextDate(Map<Long, LocalDate> nextDatesByAnimeId,
                             AnimeNextAiringDateProjection projection) {
    if (projection == null) {
      return;
    }
    mergeNextDate(nextDatesByAnimeId,
        projection.getAnimeId(),
        projection.getNextAiringDate());
  }

  private void mergeNextDate(Map<Long, LocalDate> nextDatesByAnimeId,
                             Long animeId,
                             LocalDate candidateDate) {
    if (animeId == null || candidateDate == null) {
      return;
    }
    nextDatesByAnimeId.merge(animeId, candidateDate,
        (current, candidate) -> candidate.isBefore(current) ? candidate : current);
  }

  private void mergeIntoDto(AnimeDto dto, LocalDate candidateDate) {
    if (dto == null || candidateDate == null) {
      return;
    }
    LocalDate currentDate = dto.getNextAiringDate();
    if (currentDate == null || candidateDate.isBefore(currentDate)) {
      dto.setNextAiringDate(candidateDate);
    }
  }
}
