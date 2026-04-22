package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.example.animetracker.cache.AnimeSearchCache;
import org.example.animetracker.cache.AnimeSearchKey;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.model.Anime;
import org.example.animetracker.repository.AnimeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AnimeServiceTest {

  @Mock private AnimeRepository  animeRepository;
  @Mock private AnimeSearchCache searchCache;

  @InjectMocks
  private AnimeService animeService;

  @Test
  @DisplayName("findByIdWithoutProblem — аниме не найдено → 404")
  void findByIdWithoutProblem_notFound_throws404() {
    when(animeRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> animeService.findByIdWithoutProblem(99L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Anime not found with id: 99");
  }


  @Test
  @DisplayName("findByStudioAndName — studio + title → результат")
  void findByStudioAndName_studioAndTitle_returnsList() {
    when(animeRepository.findByStudioAndTitle("MAPPA", "Attack on Titan"))
        .thenReturn(List.of(buildAnime(1L)));

    List<AnimeDto> result = animeService.findByStudioAndName("MAPPA", "Attack on Titan");

    assertThat(result).hasSize(1);
    verify(animeRepository).findByStudioAndTitle("MAPPA", "Attack on Titan");
  }

  @Test
  @DisplayName("findByStudioAndName — studio + title, ничего нет → 404")
  void findByStudioAndName_studioAndTitle_empty_throws404() {
    when(animeRepository.findByStudioAndTitle(any(), any()))
        .thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> animeService.findByStudioAndName("X", "Y"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("studio: X")
        .hasMessageContaining("title: Y");
  }

  @Test
  @DisplayName("findByStudioAndName — только studio → результат")
  void findByStudioAndName_onlyStudio_returnsList() {
    when(animeRepository.findByStudio("MAPPA")).thenReturn(List.of(buildAnime(1L)));

    List<AnimeDto> result = animeService.findByStudioAndName("MAPPA", null);

    assertThat(result).hasSize(1);
    verify(animeRepository).findByStudio("MAPPA");
  }

  @Test
  @DisplayName("findByStudioAndName — только studio, ничего нет → 404")
  void findByStudioAndName_onlyStudio_empty_throws404() {
    when(animeRepository.findByStudio("X")).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> animeService.findByStudioAndName("X", null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("studio: X");
  }

  @Test
  @DisplayName("findByStudioAndName — только title → результат")
  void findByStudioAndName_onlyTitle_returnsList() {
    when(animeRepository.findByTitle("Naruto")).thenReturn(List.of(buildAnime(2L)));

    List<AnimeDto> result = animeService.findByStudioAndName(null, "Naruto");

    assertThat(result).hasSize(1);
    verify(animeRepository).findByTitle("Naruto");
  }

  @Test
  @DisplayName("findByStudioAndName — только title, ничего нет → 404")
  void findByStudioAndName_onlyTitle_empty_throws404() {
    when(animeRepository.findByTitle("Unknown")).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> animeService.findByStudioAndName(null, "Unknown"))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("title: Unknown");
  }

  @Test
  @DisplayName("findByStudioAndName — без параметров → весь каталог")
  void findByStudioAndName_noParams_returnsAll() {
    when(animeRepository.findAll()).thenReturn(List.of(buildAnime(1L), buildAnime(2L)));

    List<AnimeDto> result = animeService.findByStudioAndName(null, null);

    assertThat(result).hasSize(2);
    verify(animeRepository).findAll();
  }

  @Test
  @DisplayName("findByStudioAndName — без параметров, каталог пуст → 404")
  void findByStudioAndName_noParams_empty_throws404() {
    when(animeRepository.findAll()).thenReturn(Collections.emptyList());

    assertThatThrownBy(() -> animeService.findByStudioAndName(null, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("No anime found");
  }


  @Test
  @DisplayName("getAllSortedByPopularity — возвращает страницу DTO")
  void getAllSortedByPopularity_returnsPage() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Anime> animePage = new PageImpl<>(List.of(buildAnime(1L)));
    when(animeRepository.findAllSorted(pageable)).thenReturn(animePage);

    Page<AnimeDto> result = animeService.getAllSortedByPopularity(pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(animeRepository).findAllSorted(pageable);
  }

  @Test
  @DisplayName("findByGenreAndMinSeasons — cache hit → репозиторий не вызывается")
  void findByGenreAndMinSeasons_cacheHit_skipRepository() {
    Pageable pageable = PageRequest.of(0, 5);
    AnimeSearchKey key = new AnimeSearchKey("Action", 1, 0, 5, pageable.getSort());
    Page<AnimeDto> cached = new PageImpl<>(List.of(new AnimeDto(1L, "Solo Leveling", 1, "A-1", true, true, null)));

    when(searchCache.get(key)).thenReturn(cached);

    Page<AnimeDto> result = animeService.findByGenreAndMinSeasons("Action", 1, pageable);

    assertThat(result).isSameAs(cached);
    verify(animeRepository, never()).findByGenreAndMinSeasons(any(), any(int.class), any());
  }

  @Test
  @DisplayName("findByGenreAndMinSeasons — cache miss → репозиторий + сохранение в кеш")
  void findByGenreAndMinSeasons_cacheMiss_callsRepositoryAndCaches() {
    Pageable pageable = PageRequest.of(0, 5);
    AnimeSearchKey key = new AnimeSearchKey("Action", 2, 0, 5, pageable.getSort());
    Page<Anime> dbPage = new PageImpl<>(List.of(buildAnime(1L)));

    when(searchCache.get(key)).thenReturn(null);
    when(animeRepository.findByGenreAndMinSeasons("Action", 2, pageable)).thenReturn(dbPage);

    Page<AnimeDto> result = animeService.findByGenreAndMinSeasons("Action", 2, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(searchCache).put(eq(key), any());
  }

  @Test
  @DisplayName("findByGenreAndMinSeasonsNative — cache hit → репозиторий не вызывается")
  void findByGenreAndMinSeasonsNative_cacheHit_skipRepository() {
    Pageable pageable = PageRequest.of(0, 5);
    AnimeSearchKey key = new AnimeSearchKey("Fantasy", 3, 0, 5, pageable.getSort());
    Page<AnimeDto> cached = new PageImpl<>(List.of(new AnimeDto(1L, "Solo Leveling", 1, "A-1", true, true, null)));

    when(searchCache.get(key)).thenReturn(cached);

    Page<AnimeDto> result = animeService.findByGenreAndMinSeasonsNative("Fantasy", 3, pageable);

    assertThat(result).isSameAs(cached);
    verify(animeRepository, never()).findByGenreAndMinSeasonsNative(any(), any(int.class), any());
  }

  @Test
  @DisplayName("findByGenreAndMinSeasonsNative — cache miss → native репозиторий + кеш")
  void findByGenreAndMinSeasonsNative_cacheMiss_callsRepositoryAndCaches() {
    Pageable pageable = PageRequest.of(0, 5);
    AnimeSearchKey key = new AnimeSearchKey("Fantasy", 1, 0, 5, pageable.getSort());
    Page<Anime> dbPage = new PageImpl<>(List.of(buildAnime(2L)));

    when(searchCache.get(key)).thenReturn(null);
    when(animeRepository.findByGenreAndMinSeasonsNative("Fantasy", 1, pageable))
        .thenReturn(dbPage);

    Page<AnimeDto> result = animeService.findByGenreAndMinSeasonsNative("Fantasy", 1, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(searchCache).put(eq(key), any());
  }

  private Anime buildAnime(Long id) {
    Anime anime = new Anime();
    anime.setId(id);
    anime.setTitle("Anime #" + id);
    return anime;
  }

  @Test
  @DisplayName("findByTitle — делегирует вызов в репозиторий и возвращает Optional<Anime>")
  void findByTitle_returnsOptionalAnime() {
    Anime expectedAnime = new Anime();
    expectedAnime.setTitle("Jujutsu Kaisen");

    when(animeRepository.findByTitleIgnoreCase("Jujutsu Kaisen"))
        .thenReturn(Optional.of(expectedAnime));

    Optional<Anime> result = animeService.findByTitle("Jujutsu Kaisen");

    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("Jujutsu Kaisen");
    verify(animeRepository).findByTitleIgnoreCase("Jujutsu Kaisen");
  }

  @Test
  @DisplayName("findByTitle — возвращает empty, если аниме не найдено")
  void findByTitle_returnsEmptyWhenNotFound() {
    when(animeRepository.findByTitleIgnoreCase("Unknown Anime"))
        .thenReturn(Optional.empty());

    Optional<Anime> result = animeService.findByTitle("Unknown Anime");

    assertThat(result).isEmpty();
    verify(animeRepository).findByTitleIgnoreCase("Unknown Anime");
  }
}