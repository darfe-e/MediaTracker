package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.repository.EpisodeRepository;
import org.example.animetracker.repository.SeasonRepository;
import org.example.animetracker.repository.projection.AnimeNextAiringDateProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnimeNextAiringDateServiceTest {

  @Mock private SeasonRepository seasonRepository;
  @Mock private EpisodeRepository episodeRepository;

  @InjectMocks
  private AnimeNextAiringDateService animeNextAiringDateService;

  @Test
  @DisplayName("enrich вЂ” добавляет дату анонса и ближайшую дату серии")
  void enrich_setsNextAiringDateForAnnouncedAndOngoing() {
    AnimeDto ongoing = new AnimeDto(1L, "Dr. Stone", 7, "TMS", true, false, null);
    AnimeDto announced = new AnimeDto(2L, "Lord of Mysteries", 0, "B.CMAY", false, true, null);

    when(seasonRepository.findUpcomingSeasonDatesByAnimeIds(eq(List.of(1L, 2L)), any(LocalDate.class)))
        .thenReturn(List.of(
            projection(1L, LocalDate.of(2026, 5, 3)),
            projection(2L, LocalDate.of(2027, 1, 1))
        ));
    when(episodeRepository.findNextEpisodeDatesByAnimeIds(eq(List.of(1L, 2L)), any(LocalDate.class)))
        .thenReturn(List.of(projection(1L, LocalDate.of(2026, 4, 30))));

    List<AnimeDto> result = animeNextAiringDateService.enrich(List.of(ongoing, announced));

    assertThat(result.get(0).getNextAiringDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    assertThat(result.get(1).getNextAiringDate()).isEqualTo(LocalDate.of(2027, 1, 1));
  }

  @Test
  @DisplayName("enrich вЂ” не делает запросы для завершенных тайтлов")
  void enrich_skipsFinishedAnime() {
    AnimeDto finished = new AnimeDto(3L, "Finished", 1, "Studio", false, false, null);

    List<AnimeDto> result = animeNextAiringDateService.enrich(List.of(finished));

    assertThat(result.get(0).getNextAiringDate()).isNull();
    verify(seasonRepository, never()).findUpcomingSeasonDatesByAnimeIds(any(), any(LocalDate.class));
    verify(episodeRepository, never()).findNextEpisodeDatesByAnimeIds(any(), any(LocalDate.class));
  }

  private AnimeNextAiringDateProjection projection(Long animeId, LocalDate nextAiringDate) {
    return new AnimeNextAiringDateProjection() {
      @Override
      public Long getAnimeId() {
        return animeId;
      }

      @Override
      public LocalDate getNextAiringDate() {
        return nextAiringDate;
      }
    };
  }
}
