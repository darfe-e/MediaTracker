package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.animetracker.cache.AnimeSearchCache;
import org.example.animetracker.dto.external.*;
import org.example.animetracker.exception.AnimeImportException;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.ImportTask;
import org.example.animetracker.model.Season;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.EpisodeRepository;
import org.example.animetracker.repository.GenreRepository;
import org.example.animetracker.repository.SeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class AnimeImportServiceTest {

  @Mock private AnimeRepository    animeRepository;
  @Mock private GenreRepository    genreRepository;
  @Mock private SeasonRepository   seasonRepository;
  @Mock private EpisodeRepository  episodeRepository;
  @Mock private RestTemplate       restTemplate;
  @Mock private AnimeSearchCache   searchCache;
  @Mock private AnimeImportService self;

  private ObjectMapper objectMapper;
  private AnimeImportService service;

  private static final String FINISHED_TV_NO_EPS_JSON = """
      {
        "data": {
          "Media": {
            "id": 101,
            "idMal": null,
            "popularity": 500,
            "title": {"romaji": "Test Anime", "english": "Test Anime EN"},
            "format": "TV",
            "status": "FINISHED",
            "episodes": 0,
            "duration": 24,
            "startDate": {"year": 2020, "month": 4, "day": 1},
            "studios": {"edges": [{"node": {"name": "Test Studio"}}]},
            "genres": ["Action", "Adventure"],
            "airingSchedule": {"nodes": []},
            "relations": {"edges": []}
          }
        }
      }
      """;

  /**
   * episodes=12 → hasEpisodes() == true → registerNode добавит медиа в allMedia.
   * Используется в тестах, вызывающих importFromApi() / refreshPopularAnime().
   */
  private static final String FINISHED_TV_WITH_EPS_JSON = """
      {
        "data": {
          "Media": {
            "id": 101,
            "idMal": null,
            "popularity": 500,
            "title": {"romaji": "Test Anime", "english": "Test Anime EN"},
            "format": "TV",
            "status": "FINISHED",
            "episodes": 12,
            "duration": 24,
            "startDate": {"year": 2020, "month": 4, "day": 1},
            "studios": {"edges": [{"node": {"name": "Test Studio"}}]},
            "genres": ["Action"],
            "airingSchedule": {"nodes": []},
            "relations": {"edges": []}
          }
        }
      }
      """;

  /** RELEASING + airingSchedule → resolveEpisodeCount использует maxEpisodeFromSchedule. */
  private static final String RELEASING_WITH_SCHEDULE_JSON = """
      {
        "data": {
          "Media": {
            "id": 202,
            "idMal": null,
            "popularity": 800,
            "title": {"romaji": "Ongoing Anime", "english": null},
            "format": "TV",
            "status": "RELEASING",
            "episodes": null,
            "duration": 23,
            "startDate": {"year": 2024, "month": 1, "day": null},
            "studios": {"edges": []},
            "genres": [],
            "airingSchedule": {"nodes": [
              {"airingAt": 1700000000, "episode": 5},
              {"airingAt": 1700600000, "episode": 6}
            ]},
            "relations": {"edges": []}
          }
        }
      }
      """;

  /** Формат MUSIC — не проходит isAcceptableFormat(), allMedia остаётся пустым. */
  private static final String MUSIC_FORMAT_JSON = """
      {
        "data": {
          "Media": {
            "id": 303,
            "idMal": null,
            "popularity": 100,
            "title": {"romaji": "Music Video", "english": null},
            "format": "MUSIC",
            "status": "FINISHED",
            "episodes": 1,
            "duration": 5,
            "startDate": null,
            "studios": {"edges": []},
            "genres": [],
            "airingSchedule": {"nodes": []},
            "relations": {"edges": []}
          }
        }
      }
      """;

  /** episodes=12, idMal=123 — для тестов Jikan. */
  private static final String TV_WITH_MAL_ID_JSON = """
      {
        "data": {
          "Media": {
            "id": 404,
            "idMal": 123,
            "popularity": 300,
            "title": {"romaji": "Test With Mal", "english": "Test With Mal EN"},
            "format": "TV",
            "status": "FINISHED",
            "episodes": 2,
            "duration": 24,
            "startDate": {"year": 2021, "month": 6, "day": 15},
            "studios": {"edges": []},
            "genres": [],
            "airingSchedule": {"nodes": []},
            "relations": {"edges": []}
          }
        }
      }
      """;

  /** Page с одним элементом (episodes=12) для refreshPopularAnime. */
  private static final String PAGE_ONE_ITEM_JSON = """
      {
        "data": {
          "Page": {
            "media": [{
              "id": 101,
              "idMal": null,
              "popularity": 500,
              "title": {"romaji": "Test Anime", "english": "Test Anime EN"},
              "format": "TV",
              "status": "FINISHED",
              "episodes": 12,
              "duration": 24,
              "startDate": {"year": 2020, "month": 4, "day": 1},
              "studios": {"edges": [{"node": {"name": "Test Studio"}}]},
              "genres": ["Action"],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }]
          }
        }
      }
      """;

  /**
   * Стартовое медиа с SEQUEL-связью на ID 501.
   * Используется для тестирования обхода цепочки franchise (collectFullChain).
   */
  private static final String MEDIA_WITH_SEQUEL_JSON = """
      {
        "data": {
          "Media": {
            "id": 500,
            "idMal": null,
            "popularity": 900,
            "title": {"romaji": "Start Anime", "english": "Start Anime EN"},
            "format": "TV",
            "status": "FINISHED",
            "episodes": 12,
            "duration": 24,
            "startDate": {"year": 2020, "month": 1, "day": 1},
            "studios": {"edges": []},
            "genres": [],
            "airingSchedule": {"nodes": []},
            "relations": {
              "edges": [
                {"relationType": "SEQUEL", "node": {"id": 501}}
              ]
            }
          }
        }
      }
      """;

  /**
   * Сиквел-медиа (ID 501) с обратной PREQUEL-ссылкой на 500.
   * Обратная ссылка проверяет защиту от бесконечных циклов (visited.contains).
   */
  private static final String SEQUEL_MEDIA_JSON = """
      {
        "data": {
          "Media": {
            "id": 501,
            "idMal": null,
            "popularity": 700,
            "title": {"romaji": "Sequel Anime", "english": null},
            "format": "TV",
            "status": "FINISHED",
            "episodes": 8,
            "duration": 24,
            "startDate": {"year": 2021, "month": 4, "day": 1},
            "studios": {"edges": []},
            "genres": [],
            "airingSchedule": {"nodes": []},
            "relations": {
              "edges": [
                {"relationType": "PREQUEL", "node": {"id": 500}}
              ]
            }
          }
        }
      }
      """;

  /** TV_SHORT — isTvFormat возвращает true для этого формата. */
  private static final String TV_SHORT_JSON = """
      {
        "data": {
          "Media": {
            "id": 600,
            "idMal": null,
            "popularity": 150,
            "title": {"romaji": "Short Anime", "english": null},
            "format": "TV_SHORT",
            "status": "FINISHED",
            "episodes": 12,
            "duration": 5,
            "startDate": {"year": 2022, "month": 3, "day": 1},
            "studios": {"edges": []},
            "genres": [],
            "airingSchedule": {"nodes": []},
            "relations": {"edges": []}
          }
        }
      }
      """;

  /**
   * NOT_YET_RELEASED + airingSchedule с одним эпизодом.
   * isReleasingOrUpcoming → true, resolveEpisodeCount → maxEpisodeFromSchedule = 1.
   */
  private static final String NOT_YET_RELEASED_JSON = """
      {
        "data": {
          "Media": {
            "id": 700,
            "idMal": null,
            "popularity": 600,
            "title": {"romaji": "Upcoming Anime", "english": null},
            "format": "TV",
            "status": "NOT_YET_RELEASED",
            "episodes": null,
            "duration": 24,
            "startDate": {"year": 2025, "month": 4, "day": null},
            "studios": {"edges": []},
            "genres": [],
            "airingSchedule": {"nodes": [
              {"airingAt": 1900000000, "episode": 1}
            ]},
            "relations": {"edges": []}
          }
        }
      }
      """;

  /** OVA-формат — проходит isAcceptableFormat(). */
  private static final String OVA_WITH_EPS_JSON = """
      {
        "data": {
          "Media": {
            "id": 800,
            "idMal": null,
            "popularity": 200,
            "title": {"romaji": "Test OVA", "english": null},
            "format": "OVA",
            "status": "FINISHED",
            "episodes": 2,
            "duration": 30,
            "startDate": {"year": 2019, "month": 6, "day": 1},
            "studios": {"edges": []},
            "genres": [],
            "airingSchedule": {"nodes": []},
            "relations": {"edges": []}
          }
        }
      }
      """;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new AnimeImportService(
        animeRepository, genreRepository, seasonRepository,
        episodeRepository, restTemplate, objectMapper, self, searchCache);
  }

  @Test
  @DisplayName("saveFranchise — новое аниме, episodes=0 → сохраняется без эпизодов")
  void saveFranchise_newAnime_noEpisodes_savedSuccessfully() throws Exception {
    var anilistMedia = parseMedia(FINISHED_TV_NO_EPS_JSON);

    Anime savedAnime = buildSavedAnime(1L, "Test Anime EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(genreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Test Anime EN");
    verify(animeRepository).save(any());
    verify(searchCache).invalidateAll();
    verify(episodeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — существующее аниме → обновляется через findByExternalId")
  void saveFranchise_existingAnime_updatesExisting() throws Exception {
    var anilistMedia = parseMedia(FINISHED_TV_NO_EPS_JSON);

    Anime existing = buildSavedAnime(99L, "Old Title");
    Season savedSeason = buildSavedSeason(5L);

    when(animeRepository.findByExternalId(101L)).thenReturn(Optional.of(existing));
    when(genreRepository.findAll()).thenReturn(List.of());
    when(genreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(animeRepository.save(any())).thenReturn(existing);
    when(seasonRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
    verify(animeRepository).save(existing);
  }

  @Test
  @DisplayName("saveFranchise — нет TV-медиа → берёт первый элемент (MUSIC)")
  void saveFranchise_noTvFormat_takesFirstElement() throws Exception {
    var anilistMedia = parseMedia(MUSIC_FORMAT_JSON);

    Anime savedAnime = buildSavedAnime(1L, "Music Video");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(303L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(303L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    // episodes=1 → saveAll вызывается
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 0, false, false);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("saveFranchise — RELEASING + airingSchedule → resolveEpisodeCount использует schedule")
  void saveFranchise_releasingWithSchedule_resolvesEpisodeCountFromSchedule() throws Exception {
    var anilistMedia = parseMedia(RELEASING_WITH_SCHEDULE_JSON);

    Anime savedAnime = buildSavedAnime(2L, "Ongoing Anime");
    Season savedSeason = buildSavedSeason(20L);

    // idMal=null → Jikan не вызывается
    when(animeRepository.findByExternalId(202L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(202L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, true, true);

    assertThat(result).isNotNull();
    // totalEps = maxEpisodeFromSchedule = 6 → saveAll вызывается
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — сезон уже есть в БД → обновляется, а не создаётся")
  void saveFranchise_existingSeason_updates() throws Exception {
    var anilistMedia = parseMedia(FINISHED_TV_NO_EPS_JSON);

    Anime savedAnime = buildSavedAnime(1L, "Test Anime EN");
    Season existing = buildSavedSeason(77L);
    existing.setExternalId(101L);

    when(animeRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(genreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(101L)).thenReturn(Optional.of(existing));
    when(seasonRepository.save(any())).thenReturn(existing);

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
    verify(seasonRepository).save(existing);
  }

  @Test
  @DisplayName("saveFranchise — новые жанры сохраняются через genreRepository.save")
  void saveFranchise_newGenres_savedViaRepository() throws Exception {
    // FINISHED_TV_NO_EPS_JSON содержит genres: ["Action","Adventure"]
    var anilistMedia = parseMedia(FINISHED_TV_NO_EPS_JSON);

    Anime savedAnime = buildSavedAnime(1L, "Test Anime EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of()); // пустой кэш
    when(genreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    service.saveFranchise(List.of(anilistMedia), 1, false, false);

    // 2 новых жанра → save вызывается 2 раза
    verify(genreRepository, times(2)).save(any());
  }

  @Test
  @DisplayName("importFromApi — медиа найдена (episodes=12), saveFranchise → present")
  void importFromApi_validMedia_saveFranchiseReturnsAnime_returnsPresent() {
    // episodes=12 → hasEpisodes() == true → registerNode добавляет в allMedia
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_WITH_EPS_JSON));

    Anime expected = buildSavedAnime(1L, "Test Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenReturn(expected);

    Optional<Anime> result = service.importFromApi("Test Anime");

    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("Test Anime EN");
  }

  @Test
  @DisplayName("importFromApi — saveFranchise выбрасывает → Optional.empty")
  void importFromApi_saveFranchiseThrows_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_WITH_EPS_JSON));

    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenThrow(new RuntimeException("DB error"));

    Optional<Anime> result = service.importFromApi("Test Anime");

    // исключение перехватывается catch-блоком importFromApi → empty
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("importFromApi — saveFranchise возвращает null → Optional.empty")
  void importFromApi_saveFranchiseReturnsNull_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_WITH_EPS_JSON));

    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenReturn(null);

    Optional<Anime> result = service.importFromApi("Test Anime");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("importFromApi — формат MUSIC (неприемлемый) → allMedia пуст → empty")
  void importFromApi_unacceptableFormat_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(MUSIC_FORMAT_JSON));

    Optional<Anime> result = service.importFromApi("Music Video");

    // MUSIC → isAcceptableFormat == false → allMedia пуст → processFranchise null
    assertThat(result).isEmpty();
    verify(self, never()).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
  }

  @Test
  @DisplayName("importFromApi — AniList вернул 500 → executeAnilistQuery бросает → empty")
  void importFromApi_anilistHttpError_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error"));

    Optional<Anime> result = service.importFromApi("SomeTitle");

    // IllegalStateException поймано в fetchAnilistByTitle → null → empty
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("refreshPopularAnime — один элемент (episodes=12) → self.saveFranchise вызывается")
  void refreshPopularAnime_oneItem_processesNode() {
    // episodes=12 → hasEpisodes() == true → медиа попадает в allMedia
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    Anime anime = buildSavedAnime(1L, "Test Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenReturn(anime);

    service.refreshPopularAnime(1);

    verify(self).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
  }

  @Test
  @DisplayName("refreshPopularAnime — processMediaNode ловит исключение, не пробрасывает")
  void refreshPopularAnime_processingThrows_doesNotPropagate() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    // saveFranchise выбрасывает → processMediaNode ловит как Exception
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenThrow(new RuntimeException("processing error"));

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> service.refreshPopularAnime(1)
    );
  }

  @Test
  @DisplayName("refreshPopularAnime — processMediaNode: Thread.sleep(3000) прерван → IllegalState перехватывается")
  void refreshPopularAnime_processMediaNodeInterrupted_doesNotPropagate() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenAnswer(inv -> {
          Thread.currentThread().interrupt();
          return buildSavedAnime(1L, "Test Anime EN");
        });

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> service.refreshPopularAnime(1)
    );

    // Очищаем флаг прерывания — он мог остаться после re-interrupt в catch-блоке
    Thread.interrupted();
  }

  @Test
  @DisplayName("importFromApi — SEQUEL-связь: связанная запись найдена → allMedia содержит 2 элемента")
  void importFromApi_sequelRelation_relatedMediaFound() {

    AtomicInteger callCount = new AtomicInteger(0);
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          int call = callCount.incrementAndGet();
          return call == 1
              ? ResponseEntity.ok(MEDIA_WITH_SEQUEL_JSON)
              : ResponseEntity.ok(SEQUEL_MEDIA_JSON);
        });

    Anime anime = buildSavedAnime(1L, "Start Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    assertThat(result).isPresent();
    // saveFranchise вызван ровно один раз с assembled franchise
    verify(self).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
  }

  @Test
  @DisplayName("importFromApi — SEQUEL-связь: связанная запись null → log.warn, продолжает с основным")
  void importFromApi_sequelRelation_relatedMediaNull_logWarn() {

    AtomicInteger callCount = new AtomicInteger(0);
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          int call = callCount.incrementAndGet();
          return call == 1
              ? ResponseEntity.ok(MEDIA_WITH_SEQUEL_JSON)
              : ResponseEntity.ok("{\"data\":{\"Media\":null}}");
        });

    Anime anime = buildSavedAnime(1L, "Start Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    assertThat(result).isPresent();
    // Только стартовая запись в allMedia, saveFranchise вызван
    verify(self).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
  }

  @Test
  @DisplayName("importFromApi — retry-sleep прерывается → fetchAnilistByIdWithRetry возвращает null")
  void importFromApi_fetchAnilistByIdRetry_interruptedDuringSleep_returnsNull() {

    AtomicInteger callCount = new AtomicInteger(0);
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          int call = callCount.incrementAndGet();
          if (call == 1) {
            return ResponseEntity.ok(MEDIA_WITH_SEQUEL_JSON);
          }
          Thread.currentThread().interrupt();
          throw new RuntimeException("network error");
        });

    Anime anime = buildSavedAnime(1L, "Start Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    Thread.interrupted();

    assertThat(result).isPresent();
  }

  @Test
  @DisplayName("importFromApi — episodes=0 → hasEpisodes false → allMedia пуст → Optional.empty")
  void importFromApi_episodesZero_hasEpisodesFalse_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_NO_EPS_JSON));

    Optional<Anime> result = service.importFromApi("Test Anime");

    assertThat(result).isEmpty();
    verify(self, never()).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
  }

  @Test
  @DisplayName("importFromApi — формат OVA → isAcceptableFormat true → входит в allMedia")
  void importFromApi_ovaFormat_isAcceptableFormat() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(OVA_WITH_EPS_JSON));

    Anime anime = buildSavedAnime(1L, "Test OVA");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class)))
        .thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Test OVA");

    assertThat(result).isPresent();
    // OVA → isAcceptableFormat=true → allMedia содержит медиа → saveFranchise вызван
    verify(self).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
  }

  @Test
  @DisplayName("saveFranchise — формат TV_SHORT → isTvFormat true, медиа выбирается как root")
  void saveFranchise_tvShortFormat_isTvFormatTrue() throws Exception {
    var anilistMedia = parseMedia(TV_SHORT_JSON);

    Anime savedAnime = buildSavedAnime(1L, "Short Anime");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(600L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(600L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
    verify(animeRepository).save(any());
  }

  @Test
  @DisplayName("saveFranchise — статус NOT_YET_RELEASED → isReleasingOrUpcoming true, resolveEpisodeCount из schedule")
  void saveFranchise_notYetReleased_isReleasingOrUpcoming() throws Exception {
    var anilistMedia = parseMedia(NOT_YET_RELEASED_JSON);

    Anime savedAnime = buildSavedAnime(1L, "Upcoming Anime");
    Season savedSeason = buildSavedSeason(1L);

    // idMal=null → Jikan не вызывается
    when(animeRepository.findByExternalId(700L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(700L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 0, true, true);

    assertThat(result).isNotNull();
    // totalEps = maxEpisodeFromSchedule = 1 → saveAll вызывается
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — idMal != null, Jikan возвращает эпизоды с title и romanji")
  void saveFranchise_withMalId_jikanReturnsEpisodes() throws Exception {
    var anilistMedia = parseMedia(TV_WITH_MAL_ID_JSON);

    String jikanResponse = """
        {
          "data": [
            {"mal_id": 1, "title": "Episode 1 Title", "title_romanji": null},
            {"mal_id": 2, "title": null, "title_romanji": "Ep 2 Romanji"}
          ],
          "pagination": {"has_next_page": false}
        }
        """;

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jikanResponse));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
    verify(episodeRepository).saveAll(any());
    verify(restTemplate).getForEntity(any(String.class), eq(String.class));
  }

  @Test
  @DisplayName("saveFranchise — Jikan возвращает пустой data → эпизоды без названий")
  void saveFranchise_jikanEmptyData_episodesTitlesDefault() throws Exception {
    var anilistMedia = parseMedia(TV_WITH_MAL_ID_JSON);

    String jikanEmpty = """
        {
          "data": [],
          "pagination": {"has_next_page": false}
        }
        """;

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jikanEmpty));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — Jikan вернул не-2xx → fetchJikanPage возвращает false")
  void saveFranchise_jikanNon2xx_handledGracefully() throws Exception {
    var anilistMedia = parseMedia(TV_WITH_MAL_ID_JSON);

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(null));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("saveFranchise — Jikan выбрасывает исключение → обрабатывается без пробрасывания")
  void saveFranchise_jikanThrows_handledGracefully() throws Exception {
    var anilistMedia = parseMedia(TV_WITH_MAL_ID_JSON);

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenThrow(new RuntimeException("Jikan timeout"));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
    // Эпизоды сохраняются с заголовками "Episode N" (по умолчанию)
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — Jikan: поток прерван до Thread.sleep(400) → InterruptedException перехватывается")
  void saveFranchise_jikanInterrupted_doesNotThrow() throws Exception {
    var anilistMedia = parseMedia(TV_WITH_MAL_ID_JSON); // idMal=123, episodes=2

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Thread.currentThread().interrupt();

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    Thread.interrupted();

    assertThat(result).isNotNull();
    verify(restTemplate, never()).getForEntity(any(String.class), eq(String.class));
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — Jikan has_next_page=true → загружает несколько страниц")
  void saveFranchise_jikanHasNextPage_fetchesMultiplePages() throws Exception {
    // 200 эпизодов → maxPages = ceil(200/100.0) = 2
    String mediaJson = """
        {
          "data": {
            "Media": {
              "id": 405, "idMal": 124, "popularity": 300,
              "title": {"romaji": "Long Anime", "english": "Long Anime EN"},
              "format": "TV", "status": "FINISHED",
              "episodes": 200, "duration": 24,
              "startDate": {"year": 2010, "month": 1, "day": 1},
              "studios": {"edges": []}, "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var anilistMedia = parseMedia(mediaJson);

    String jikanPage1 = """
        {"data": [{"mal_id": 1, "title": "Ep 1", "title_romanji": null}],
         "pagination": {"has_next_page": true}}
        """;
    String jikanPage2 = """
        {"data": [{"mal_id": 101, "title": "Ep 101", "title_romanji": null}],
         "pagination": {"has_next_page": false}}
        """;

    Anime savedAnime = buildSavedAnime(1L, "Long Anime EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(405L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(405L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    AtomicInteger jikanCallCount = new AtomicInteger(0);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenAnswer(inv -> {
          int call = jikanCallCount.incrementAndGet();
          return call == 1 ? ResponseEntity.ok(jikanPage1) : ResponseEntity.ok(jikanPage2);
        });
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false, false);

    assertThat(result).isNotNull();
    // page=1 (has_next_page=true) + page=2 (has_next_page=false) → 2 запроса
    verify(restTemplate, times(2)).getForEntity(any(String.class), eq(String.class));
  }

  @Test
  @DisplayName("saveFranchise — жанры null и пустая строка в списке → пропускаются (continue)")
  void saveFranchise_nullOrBlankGenresInList_skipped() throws Exception {
    // genres: ["Action", null, ""] — null и "" должны быть пропущены
    String mediaJson = """
        {
          "data": {
            "Media": {
              "id": 900, "idMal": null, "popularity": 100,
              "title": {"romaji": "Genre Test", "english": null},
              "format": "TV", "status": "FINISHED",
              "episodes": 0, "duration": 24,
              "startDate": null,
              "studios": {"edges": []},
              "genres": ["Action", null, ""],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var anilistMedia = parseMedia(mediaJson);

    Anime savedAnime = buildSavedAnime(1L, "Genre Test");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(900L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(genreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(900L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    Anime result = service.saveFranchise(List.of(anilistMedia), 0, false, false);

    assertThat(result).isNotNull();
    // Только "Action" не пустой → genreRepository.save вызван ровно 1 раз
    verify(genreRepository, times(1)).save(any());
    // episodes=0 → saveAll не вызывается
    verify(episodeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("importFromApi — format=null → isAcceptableFormat false → allMedia пуст → empty")
  void importFromApi_nullFormat_isAcceptableFormatNullGuard_returnsEmpty() {
    String json = """
        {
          "data": {
            "Media": {
              "id": 1001, "idMal": null, "popularity": 100,
              "title": {"romaji": "Null Format Anime", "english": null},
              "format": null, "status": "FINISHED",
              "episodes": 5, "duration": 24,
              "startDate": null,
              "studios": {"edges": []}, "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(json));

    Optional<Anime> result = service.importFromApi("Null Format Anime");

    // format=null → isAcceptableFormat null-guard hits → false → allMedia пуст → processFranchise null
    assertThat(result).isEmpty();
    verify(self, never()).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
  }

  @Test
  @DisplayName("saveFranchise — format=null → isTvFormat false, берёт первый элемент как root")
  void saveFranchise_nullFormatMedia_isTvFormatNullGuard_returnsFalse() throws Exception {
    String json = """
        {
          "data": {
            "Media": {
              "id": 1002, "idMal": null, "popularity": 100,
              "title": {"romaji": "Null Format", "english": null},
              "format": null, "status": "FINISHED",
              "episodes": 0, "duration": 24,
              "startDate": {"year": 2020, "month": 1, "day": 1},
              "studios": {"edges": []}, "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var media = parseMedia(json);

    Anime savedAnime = buildSavedAnime(1L, "Null Format");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(1002L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(1002L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    // format=null → isTvFormat=false → фильтр TV пуст → orElse(allMedia.get(0))
    Anime result = service.saveFranchise(List.of(media), 0, false, false);

    assertThat(result).isNotNull();
    verify(animeRepository).findByExternalId(1002L);
    verify(episodeRepository, never()).saveAll(any()); // episodes=0
  }

  @Test
  @DisplayName("saveFranchise — status=null → isReleasingOrUpcoming false, episodes=0 → нет эпизодов")
  void saveFranchise_nullStatus_isReleasingOrUpcomingNullGuard_returnsFalse() throws Exception {
    String json = """
        {
          "data": {
            "Media": {
              "id": 1003, "idMal": null, "popularity": 100,
              "title": {"romaji": "Null Status", "english": null},
              "format": "TV", "status": null,
              "episodes": null, "duration": 24,
              "startDate": {"year": 2020, "month": 1, "day": 1},
              "studios": {"edges": []}, "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var media = parseMedia(json);

    Anime savedAnime = buildSavedAnime(1L, "Null Status");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(1003L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(1003L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    Anime result = service.saveFranchise(List.of(media), 0, false, false);

    assertThat(result).isNotNull();
    verify(episodeRepository, never()).saveAll(any());
  }

  static Stream<Arguments> provideInvalidOrIgnoredRelations() {
    return Stream.of(
        Arguments.of("relations=null", "null"),
        Arguments.of("relations.edges=null", "{\"edges\": null}"),
        Arguments.of("null edge в edges[]", "{\"edges\": [null]}"),
        Arguments.of("edge.node=null", "{\"edges\": [{\"relationType\": \"SEQUEL\", \"node\": null}]}"),
        Arguments.of("relationType=ADAPTATION (!isSequelOrPrequel)", "{\"edges\": [{\"relationType\": \"ADAPTATION\", \"node\": {\"id\": 9999}}]}")
    );
  }

  @ParameterizedTest(name = "importFromApi — {0} → continue/return, не падает и не добавляет в очередь")
  @MethodSource("provideInvalidOrIgnoredRelations")
  void importFromApi_invalidOrIgnoredRelations_skipsAndOnlyFetchesOnce(String testName, String relationsJson) {
    String json = """
      {
        "data": {
          "Media": {
            "id": 1000, "idMal": null, "popularity": 100,
            "title": {"romaji": "Test Anime", "english": "Test Anime EN"},
            "format": "TV", "status": "FINISHED",
            "episodes": 5, "duration": 24,
            "startDate": {"year": 2020, "month": 1, "day": 1},
            "studios": {"edges": []}, "genres": [],
            "airingSchedule": {"nodes": []},
            "relations": %s
          }
        }
      }
      """.formatted(relationsJson);

    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(json));

    Anime anime = buildSavedAnime(1L, "Test Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class))).thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Test Anime");

    // Убеждаемся, что парсинг прошел успешно и очередь toFetch осталась пустой
    // (нет дополнительных вызовов API для сиквелов)
    assertThat(result).isPresent();
    verify(restTemplate, times(1))
        .exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class));
  }

  @Test
  @DisplayName("registerNode — fetchAnilistByIdWithRetry вернул медиа с уже посещённым ID → пропускается")
  void importFromApi_fetchedSequelHasAlreadyVisitedId_registerNodeSkips() {
    AtomicInteger callCount = new AtomicInteger(0);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          int call = callCount.incrementAndGet();
          if (call == 1) {
            return ResponseEntity.ok(MEDIA_WITH_SEQUEL_JSON); // id=500, SEQUEL→501
          }
          return ResponseEntity.ok("""
              {
                "data": {
                  "Media": {
                    "id": 500, "idMal": null, "popularity": 900,
                    "title": {"romaji": "Start Anime", "english": "Start Anime EN"},
                    "format": "TV", "status": "FINISHED", "episodes": 12, "duration": 24,
                    "startDate": {"year": 2020, "month": 1, "day": 1},
                    "studios": {"edges": []}, "genres": [],
                    "airingSchedule": {"nodes": []},
                    "relations": {"edges": []}
                  }
                }
              }
              """);
        });

    Anime anime = buildSavedAnime(1L, "Start Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class))).thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    // visited.contains(500)=true → registerNode возвращается → allMedia содержит только 500
    assertThat(result).isPresent();
    verify(self).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
    verify(restTemplate, times(2))
        .exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class));
  }

  @Test
  @DisplayName("importFromApi — SEQUEL: fetchAnilistByIdWithRetry получает null Media → return null, log.warn")
  void importFromApi_sequelFetchReturnsNullMedia_fetchByIdReturnsNull() {
    AtomicInteger callCount = new AtomicInteger(0);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          int call = callCount.incrementAndGet();
          return call == 1
              ? ResponseEntity.ok(MEDIA_WITH_SEQUEL_JSON)
              : ResponseEntity.ok("{\"data\":{\"Media\":null}}");
        });

    Anime anime = buildSavedAnime(1L, "Start Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class))).thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    assertThat(result).isPresent();
  }

  @Test
  @DisplayName("importFromApi — SEQUEL: fetchAnilistByIdWithRetry: Media отсутствует в ответе → isMissingNode")
  void importFromApi_sequelFetchReturnsMissingMediaNode_fetchByIdReturnsNull() {
    // {"data":{}} → .path("Media") → MissingNode → isMissingNode()=true → return null
    AtomicInteger callCount = new AtomicInteger(0);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          int call = callCount.incrementAndGet();
          return call == 1
              ? ResponseEntity.ok(MEDIA_WITH_SEQUEL_JSON)
              : ResponseEntity.ok("{\"data\":{}}");
        });

    Anime anime = buildSavedAnime(1L, "Start Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class))).thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    // isMissingNode()=true → return null → log.warn → allMedia содержит только стартовое медиа
    assertThat(result).isPresent();
  }

  @Test
  @DisplayName("importFromApi — все 3 retry fetchAnilistByIdWithRetry бросают → return null (line 385)")
  void importFromApi_allThreeRetriesExhausted_fetchByIdReturnsNull() {
    AtomicInteger callCount = new AtomicInteger(0);
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          int call = callCount.incrementAndGet();
          if (call == 1) {
            return ResponseEntity.ok(MEDIA_WITH_SEQUEL_JSON);
          }
          throw new RuntimeException("Network error on call " + call);
        });

    Anime anime = buildSavedAnime(1L, "Start Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class))).thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");
    assertThat(result).isPresent();
    verify(restTemplate, times(4))
        .exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class));
    verify(self).saveFranchise(any(), any(int.class), any(boolean.class), any(boolean.class));
  }

  @Test
  @DisplayName("saveFranchise — Jikan: 2xx с null body → condition body==null → return false")
  void saveFranchise_jikanOkWithNullBody_returnsFalse() throws Exception {
    var media = parseMedia(TV_WITH_MAL_ID_JSON); // idMal=123, episodes=2

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    // 2xx но body=null → getBody()==null → return false (line 341)
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok((String) null));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(media), 1, false, false);

    assertThat(result).isNotNull();
    // body=null → return false → titles пустой → дефолтные заголовки эпизодов
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — Jikan: data — объект, не массив → !data.isArray() → return false")
  void saveFranchise_jikanDataNotArray_returnsFalse() throws Exception {
    var media = parseMedia(TV_WITH_MAL_ID_JSON);

    String jikanNotArray = """
        {
          "data": {"error": "not an array"},
          "pagination": {"has_next_page": false}
        }
        """;

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jikanNotArray));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(media), 1, false, false);

    assertThat(result).isNotNull();
    // !data.isArray() → return false → эпизоды с дефолтными заголовками
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — Jikan: epNum=0 → обе ветки if/else if ложные → title не записывается")
  void saveFranchise_jikanEpisodeNumZero_neitherBranchTaken() throws Exception {
    var media = parseMedia(TV_WITH_MAL_ID_JSON); // episodes=2

    // mal_id=0 → epNum=0 → epNum > 0 false → обе ветки пропускаются
    String jikanZeroId = """
        {
          "data": [
            {"mal_id": 0, "title": "Should Be Skipped", "title_romanji": "Also Skipped"},
            {"mal_id": 1, "title": "Real Episode", "title_romanji": null}
          ],
          "pagination": {"has_next_page": false}
        }
        """;

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jikanZeroId));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(media), 1, false, false);

    assertThat(result).isNotNull();
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — english=\"\" (blank) → isBlank()=true → используется romaji")
  void saveFranchise_englishBlankString_usesRomaji() throws Exception {
    String json = """
        {
          "data": {
            "Media": {
              "id": 1010, "idMal": null, "popularity": 100,
              "title": {"romaji": "Romaji Title", "english": ""},
              "format": "TV", "status": "FINISHED",
              "episodes": 0, "duration": 24,
              "startDate": {"year": 2020, "month": 1, "day": 1},
              "studios": {"edges": []}, "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var media = parseMedia(json);

    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(1010L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    // Возвращаем аргумент (Anime с проставленным title), чтобы проверить title
    when(animeRepository.save(any())).thenAnswer(inv -> {
      Anime a = inv.getArgument(0);
      a.setId(1L);
      return a;
    });
    when(seasonRepository.findByExternalId(1010L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    Anime result = service.saveFranchise(List.of(media), 0, false, false);

    assertThat(result).isNotNull();
    // english="" → isBlank()=true → condition false → используется romaji
    assertThat(result.getTitle()).isEqualTo("Romaji Title");
    verify(episodeRepository, never()).saveAll(any()); // episodes=0
  }

  @Test
  @DisplayName("saveFranchise — studios=null → root.getStudios()==null → блок студии пропускается")
  void saveFranchise_studiosNull_studioBlockSkipped() throws Exception {
    String json = """
        {
          "data": {
            "Media": {
              "id": 1011, "idMal": null, "popularity": 100,
              "title": {"romaji": "No Studio Anime", "english": null},
              "format": "TV", "status": "FINISHED",
              "episodes": 0, "duration": 24,
              "startDate": {"year": 2020, "month": 1, "day": 1},
              "studios": null, "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var media = parseMedia(json);

    Anime savedAnime = buildSavedAnime(1L, "No Studio Anime");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(1011L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(1011L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    // studios=null → root.getStudios()==null → if false → студия не устанавливается
    Anime result = service.saveFranchise(List.of(media), 0, false, false);

    assertThat(result).isNotNull();
    verify(animeRepository).save(any());
    verify(episodeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — genres=null → if(root.getGenres() != null) false → genreRepository.save не вызывается")
  void saveFranchise_genresNull_genreLoopSkipped() throws Exception {
    String json = """
        {
          "data": {
            "Media": {
              "id": 1012, "idMal": null, "popularity": 100,
              "title": {"romaji": "No Genres Anime", "english": null},
              "format": "TV", "status": "FINISHED",
              "episodes": 0, "duration": 24,
              "startDate": {"year": 2020, "month": 1, "day": 1},
              "studios": {"edges": []},
              "genres": null,
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var media = parseMedia(json);

    Anime savedAnime = buildSavedAnime(1L, "No Genres Anime");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(1012L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(1012L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    Anime result = service.saveFranchise(List.of(media), 0, false, false);

    assertThat(result).isNotNull();
    verify(genreRepository, never()).save(any());
    verify(episodeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("importFromApi — TV+NOT_YET_RELEASED → tvCount=0 (фильтр исключает), isOngoing=true")
  void importFromApi_notYetReleasedMedia_tvCountZeroIsOngoingTrue() {

    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(NOT_YET_RELEASED_JSON));

    Anime anime = buildSavedAnime(1L, "Upcoming Anime");
    when(self.saveFranchise(any(), eq(0), eq(true), any(boolean.class))).thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Upcoming Anime");

    assertThat(result).isPresent();
    verify(self).saveFranchise(any(), eq(0), eq(true), any(boolean.class));
  }

  @Test
  @DisplayName("saveFranchise — Jikan: title=\"\" (blank) → if false → else if с романджи true")
  void saveFranchise_jikanTitleBlank_fallsToRomanjiElseBranch() throws Exception {
    var media = parseMedia(TV_WITH_MAL_ID_JSON); // idMal=123, episodes=2

    String jikanBlankTitle = """
        {
          "data": [
            {"mal_id": 1, "title": "", "title_romanji": "Ep 1 Romanji"},
            {"mal_id": 2, "title": "Good Title", "title_romanji": null}
          ],
          "pagination": {"has_next_page": false}
        }
        """;

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jikanBlankTitle));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(media), 1, false, false);

    assertThat(result).isNotNull();
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — Jikan: title=null, romanji=null → обе ветки false, title не сохраняется")
  void saveFranchise_jikanBothTitleAndRomanjiNull_neitherBranchFired() throws Exception {
    var media = parseMedia(TV_WITH_MAL_ID_JSON);

    String jikanBothNull = """
        {
          "data": [
            {"mal_id": 1, "title": null, "title_romanji": null},
            {"mal_id": 2, "title": "Good Title", "title_romanji": null}
          ],
          "pagination": {"has_next_page": false}
        }
        """;

    Anime savedAnime = buildSavedAnime(1L, "Test With Mal EN");
    Season savedSeason = buildSavedSeason(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jikanBothNull));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(media), 1, false, false);

    assertThat(result).isNotNull();
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("importFromApi — AniList: поле Media отсутствует в ответе → isMissingNode → empty")
  void importFromApi_fetchAnilistByTitle_missingMediaNode_returnsEmpty() {
    when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok("{\"data\":{}}"));

    Optional<Anime> result = service.importFromApi("SomeTitle");

    assertThat(result).isEmpty();

    verify(self, never()).saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean());
  }

  @Test
  @DisplayName("saveFranchise — studios.edges=null → root.getStudios().getEdges()!=null false → skip")
  void saveFranchise_studiosEdgesNull_studioBlockConditionFalse() throws Exception {
    String json = """
        {
          "data": {
            "Media": {
              "id": 2001, "idMal": null, "popularity": 100,
              "title": {"romaji": "Edges Null Anime", "english": null},
              "format": "TV", "status": "FINISHED",
              "episodes": 0, "duration": 24,
              "startDate": {"year": 2020, "month": 1, "day": 1},
              "studios": {"edges": null},
              "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var media = parseMedia(json);

    Anime savedAnime = buildSavedAnime(1L, "Edges Null Anime");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(2001L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(2001L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    // studios != null, edges == null → getEdges()!=null false → студия не устанавливается
    Anime result = service.saveFranchise(List.of(media), 0, false, false);

    assertThat(result).isNotNull();
    verify(animeRepository).save(any());
    verify(episodeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — studio edge.node=null → inner if false → studio не устанавливается")
  void saveFranchise_studioEdgeNullNode_innerIfFalse() throws Exception {
    String json = """
        {
          "data": {
            "Media": {
              "id": 2002, "idMal": null, "popularity": 100,
              "title": {"romaji": "Null Node Studio", "english": null},
              "format": "TV", "status": "FINISHED",
              "episodes": 0, "duration": 24,
              "startDate": {"year": 2020, "month": 1, "day": 1},
              "studios": {"edges": [{"node": null}]},
              "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var media = parseMedia(json);

    Anime savedAnime = buildSavedAnime(1L, "Null Node Studio");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(2002L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(2002L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    // edge != null (edge существует), edge.getNode() == null → inner if false → studio=null
    Anime result = service.saveFranchise(List.of(media), 0, false, false);

    assertThat(result).isNotNull();
    verify(animeRepository).save(any());
    verify(episodeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — RELEASING, episodes=null, schedule пуст → fromSchedule=0 → totalEps=0")
  void saveFranchise_releasingEmptySchedule_fromScheduleZero_noEpisodesSaved() throws Exception {
    String json = """
        {
          "data": {
            "Media": {
              "id": 2003, "idMal": null, "popularity": 100,
              "title": {"romaji": "Releasing No Schedule", "english": null},
              "format": "TV", "status": "RELEASING",
              "episodes": null, "duration": 24,
              "startDate": {"year": 2025, "month": 1, "day": 1},
              "studios": {"edges": []}, "genres": [],
              "airingSchedule": {"nodes": []},
              "relations": {"edges": []}
            }
          }
        }
        """;
    var media = parseMedia(json);

    Anime savedAnime = buildSavedAnime(1L, "Releasing No Schedule");
    Season savedSeason = buildSavedSeason(1L);

    when(animeRepository.findByExternalId(2003L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(2003L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    Anime result = service.saveFranchise(List.of(media), 1, true, true);

    assertThat(result).isNotNull();
    verify(episodeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("registerNode(null, ...) → media==null → ранний return, без NPE (image 2)")
  void privateMethod_registerNode_nullMedia_earlyReturnNoPE() throws Exception {
    var registerNode = AnimeImportService.class.getDeclaredMethod(
        "registerNode",
        org.example.animetracker.dto.external.AnilistMedia.class,
        java.util.Set.class,
        java.util.Deque.class,
        java.util.Map.class);
    registerNode.setAccessible(true);

    java.util.Set<Long>  visited  = new java.util.HashSet<>();
    java.util.Deque<Long> toFetch = new java.util.ArrayDeque<>();
    java.util.Map<Long, org.example.animetracker.dto.external.AnilistMedia> result = new java.util.LinkedHashMap<>();

    // media == null → первая ветка || срабатывает → return (без NPE)
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> registerNode.invoke(service, null, visited, toFetch, result));

    assertThat(result).isEmpty(); // ничего не добавлено
  }

  @ParameterizedTest(name = "{0}(null) → m==null → return false")
  @ValueSource(strings = {
      "isAcceptableFormat",
      "isTvFormat",
      "isReleasingOrUpcoming"
  })
  void privateMethods_nullMedia_returnsFalse(String methodName) throws Exception {
    var method = AnimeImportService.class.getDeclaredMethod(
        methodName,
        org.example.animetracker.dto.external.AnilistMedia.class);
    method.setAccessible(true);

    boolean result = (boolean) method.invoke(service, (Object) null);

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("processFranchise - startNode == null")
  void processFranchise_startNode_is_null (){
    AnilistMedia startNode = null;

    Anime result = service.processFranchise(startNode);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("collectFullChain — должен прерваться на 60 итерациях (safety limit)")
  void collectFullChain_reachesSafetyLimit() throws Exception {
    AnilistMedia startNode = new AnilistMedia();
    startNode.setId(1L);

    AnilistRelationEdge initialEdge = new AnilistRelationEdge();
    AnilistMedia linkedMedia = new AnilistMedia();
    linkedMedia.setId(2L);
    initialEdge.setNode(linkedMedia);
    initialEdge.setRelationType("SEQUEL"); // Тип, который проходит фильтрацию

    AnilistRelations initialRelations = new AnilistRelations();
    initialRelations.setEdges(List.of(initialEdge));
    startNode.setRelations(initialRelations);

    AnimeImportService spyService = Mockito.spy(service);

    java.util.concurrent.atomic.AtomicLong idGenerator = new java.util.concurrent.atomic.AtomicLong(3);

    Mockito.lenient().doAnswer(invocation -> {
      Long requestedId = invocation.getArgument(0);
      AnilistMedia m = new AnilistMedia();
      m.setId(requestedId);

      AnilistRelationEdge nextEdge = new AnilistRelationEdge();
      AnilistMedia nextMedia = new AnilistMedia();
      nextMedia.setId(idGenerator.getAndIncrement());
      nextEdge.setNode(nextMedia);
      nextEdge.setRelationType("SEQUEL");

      AnilistRelations nextRels = new AnilistRelations();
      nextRels.setEdges(List.of(nextEdge));
      m.setRelations(nextRels);

      return m;
    }).when(spyService).fetchAnilistByIdWithRetry(anyLong());

    var method = AnimeImportService.class.getDeclaredMethod("collectFullChain",
        org.example.animetracker.dto.external.AnilistMedia.class);
    method.setAccessible(true);

    List<AnilistMedia> result = (List<AnilistMedia>) method.invoke(spyService, startNode);

    assertThat(result).isNotNull();
    verify(spyService, times(60)).fetchAnilistByIdWithRetry(anyLong());
  }

  @Test
  @DisplayName("fetchJikanPage — когда title null, берет title_romanji")
  void fetchJikanPage_usesRomajiWhenTitleIsNull() throws Exception {
    String json = """
            {
              "pagination": {"has_next_page": false},
              "data": [
                {
                  "mal_id": 10,
                  "title": null,
                  "title_romanji": "Romaji Title"
                }
              ]
            }
            """;

    ResponseEntity<String> response = ResponseEntity.ok(json);
    when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(response);

    Map<Integer, String> titles = new HashMap<>();

    var method = AnimeImportService.class.getDeclaredMethod("fetchJikanPage",
        Integer.class, int.class, Map.class);
    method.setAccessible(true);

    boolean hasNext = (boolean) method.invoke(service, 123, 1, titles);

    assertThat(titles).containsEntry(10, "Romaji Title");
    assertThat(hasNext).isFalse();
  }

  @Test
  @DisplayName("fillAnimeInfo — устанавливает студию, если edge и node не null")
  void fillAnimeInfo_setsStudioWhenEdgeAndNodeArePresent() throws Exception {
    Anime anime = new Anime();
    AnilistMedia root = new AnilistMedia();
    root.setTitle(new AnilistTitle());

    AnilistStudio studio = new AnilistStudio();
    studio.setName("MAPPA");

    AnilistStudioEdge edge = new AnilistStudioEdge();
    edge.setNode(studio);

    AnilistStudios studiosContainer = new AnilistStudios();
    studiosContainer.setEdges(List.of(edge));

    root.setStudios(studiosContainer);

    // Исправлено: добавлен шестой параметр boolean.class (isAnnounced) в getDeclaredMethod
    var method = AnimeImportService.class.getDeclaredMethod("fillAnimeInfo",
        Anime.class, AnilistMedia.class, long.class, Map.class, boolean.class, boolean.class);
    method.setAccessible(true);

    // Исправлено: добавлен шестой аргумент (false) в invoke
    method.invoke(service, anime, root, 1L, new HashMap<>(), true, false);

    assertThat(anime.getStudio()).isEqualTo("MAPPA");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideStudioScenarios")
  void fillAnimeInfo_studioCoverage(String description, List<AnilistStudioEdge> edges, String expectedStudio) throws Exception {
    Anime anime = new Anime();
    AnilistMedia root = new AnilistMedia();
    root.setTitle(new AnilistTitle());

    if (edges != null) {
      AnilistStudios studios = new AnilistStudios();
      studios.setEdges(edges);
      root.setStudios(studios);
    }

    var method = AnimeImportService.class.getDeclaredMethod("fillAnimeInfo",
        Anime.class, AnilistMedia.class, long.class, Map.class, boolean.class, boolean.class);
    method.setAccessible(true);

    method.invoke(service, anime, root, 1L, new HashMap<>(), true, false);

    assertThat(anime.getStudio()).isEqualTo(expectedStudio);
  }

  static Stream<Arguments> provideStudioScenarios() {
    // 1. Успешный сценарий (edge != null && node != null)
    AnilistStudio studio = new AnilistStudio();
    studio.setName("MAPPA");
    AnilistStudioEdge validEdge = new AnilistStudioEdge();
    validEdge.setNode(studio);

    // 2. Сценарий: node == null
    AnilistStudioEdge edgeWithNullNode = new AnilistStudioEdge();
    edgeWithNullNode.setNode(null);

    return Stream.of(
        Arguments.of("Все данные заполнены -> Студия установлена", List.of(validEdge), "MAPPA"),
        Arguments.of("В списке есть null (edge == null) -> Студия не установлена", Collections.singletonList(null), null),
        Arguments.of("Edge существует, но Node внутри null -> Студия не установлена", List.of(edgeWithNullNode), null)
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("provideJikanScenarios")
  void fetchJikanPage_fullCoverage(String description, String json, int expectedSize) throws Exception {
    // Мокаем ответ от Jikan API
    ResponseEntity<String> response = ResponseEntity.ok(json);
    when(restTemplate.getForEntity(anyString(), eq(String.class))).thenReturn(response);

    Map<Integer, String> titles = new HashMap<>();

    // ВАЖНО: Соблюдайте порядок аргументов здесь и в invoke
    var method = AnimeImportService.class.getDeclaredMethod("fetchJikanPage",
        Integer.class, int.class, Map.class);
    method.setAccessible(true);

    method.invoke(service, 123, 1, titles);

    // Проверяем, сколько названий попало в мапу
    assertThat(titles).hasSize(expectedSize);
  }

  static Stream<Arguments> provideJikanScenarios() {
    return Stream.of(
        Arguments.of("Успех: title пуст, берем title_romanji",
            "{\"data\": [{\"mal_id\": 10, \"title\": null, \"title_romanji\": \"Romaji\"}], \"pagination\":{}}", 1),

        Arguments.of("Провал: epNum <= 0",
            "{\"data\": [{\"mal_id\": 0, \"title\": null, \"title_romanji\": \"Valid\"}], \"pagination\":{}}", 0),

        Arguments.of("Провал: title_romanji отсутствует (null)",
            "{\"data\": [{\"mal_id\": 11, \"title\": \"\", \"title_romanji\": null}], \"pagination\":{}}", 0),

        Arguments.of("Провал: title_romanji только из пробелов",
            "{\"data\": [{\"mal_id\": 12, \"title\": null, \"title_romanji\": \"   \"}], \"pagination\":{}}", 0)
    );
  }

  @ParameterizedTest(name = "isSequelOrPrequel: {0} -> {1}")
  @CsvSource({
      ", false",             // null case
      "SEQUEL, true",
      "prequel, true",       // case-insensitive check
      "SIDE_STORY, false",
      "OTHER, false"
  })
  void isSequelOrPrequel_coverage(String type, boolean expected) throws Exception {
    var method = AnimeImportService.class.getDeclaredMethod("isSequelOrPrequel", String.class);
    method.setAccessible(true);

    boolean result = (boolean) method.invoke(service, type);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  @DisplayName("Schedule methods — coverage for null schedules and nodes")
  void scheduleMethods_nullCoverage() throws Exception {
    AnilistMedia media = new AnilistMedia();
    media.setAiringSchedule(null); // Case 1: airingSchedule is null

    var maxMethod = AnimeImportService.class.getDeclaredMethod("maxEpisodeFromSchedule", AnilistMedia.class);
    var mapMethod = AnimeImportService.class.getDeclaredMethod("buildAirDateMap", AnilistMedia.class);
    maxMethod.setAccessible(true);
    mapMethod.setAccessible(true);

    assertThat((int) maxMethod.invoke(service, media)).isZero();
    assertThat((Map<?, ?>) mapMethod.invoke(service, media)).isEmpty();

    // Case 2: nodes is null
    AnilistAiringSchedule schedule = new AnilistAiringSchedule();
    schedule.setNodes(null);
    media.setAiringSchedule(schedule);

    assertThat((int) maxMethod.invoke(service, media)).isZero();
    assertThat((Map<?, ?>) mapMethod.invoke(service, media)).isEmpty();
  }

  @Test
  @DisplayName("Schedule methods — coverage for null values inside nodes")
  void scheduleMethods_internalNullCoverage() throws Exception {
    AnilistMedia media = new AnilistMedia();
    AnilistAiringSchedule schedule = new AnilistAiringSchedule();

    // Нода с null эпизодом или временем
    AnilistAiringScheduleNode badNode = new AnilistAiringScheduleNode();
    badNode.setEpisode(null);
    badNode.setAiringAt(null);

    // Валидная нода
    AnilistAiringScheduleNode goodNode = new AnilistAiringScheduleNode();
    goodNode.setEpisode(12);
    goodNode.setAiringAt(1712000000L); // Some timestamp

    schedule.setNodes(Arrays.asList(badNode, goodNode));
    media.setAiringSchedule(schedule);

    var maxMethod = AnimeImportService.class.getDeclaredMethod("maxEpisodeFromSchedule", AnilistMedia.class);
    var mapMethod = AnimeImportService.class.getDeclaredMethod("buildAirDateMap", AnilistMedia.class);
    maxMethod.setAccessible(true);
    mapMethod.setAccessible(true);

    // maxEpisode должен отфильтровать badNode и вернуть 12
    assertThat((int) maxMethod.invoke(service, media)).isEqualTo(12);

    Map<Integer, LocalDate> resultUpdate = (Map<Integer, LocalDate>) mapMethod.invoke(service, media);
    assertThat(resultUpdate).hasSize(1).containsKey(12);
  }

  @Test
  @DisplayName("getStartDate — full coverage for ternary operators and nulls")
  void getStartDate_fullCoverage() throws Exception {
    var method = AnimeImportService.class.getDeclaredMethod("getStartDate", AnilistMedia.class);
    method.setAccessible(true);

    // 1. Media is null
    assertThat(method.invoke(service, (Object) null)).isNull();

    // 2. StartDate object is null
    AnilistMedia media = new AnilistMedia();
    media.setStartDate(null);
    assertThat(method.invoke(service, media)).isNull();

    // 3. Year is null
    AnilistDate dateDto = new AnilistDate();
    dateDto.setYear(null);
    media.setStartDate(dateDto);
    assertThat(method.invoke(service, media)).isNull();

    // 4. Month and Day are null (Coverage for ternary: month != null ? month : 1)
    dateDto.setYear(2024);
    dateDto.setMonth(null);
    dateDto.setDay(null);

    LocalDate result = (LocalDate) method.invoke(service, media);
    assertThat(result).isEqualTo(LocalDate.of(2024, 1, 1));

    // 5. Month and Day are present
    dateDto.setMonth(5);
    dateDto.setDay(20);
    result = (LocalDate) method.invoke(service, media);
    assertThat(result).isEqualTo(LocalDate.of(2024, 5, 20));
  }

  @Test
  @DisplayName("buildAirDateMap — полное покрытие условий (комбинации null)")
  void buildAirDateMap_fullConditionCoverage() throws Exception {
    AnilistMedia media = new AnilistMedia();
    AnilistAiringSchedule schedule = new AnilistAiringSchedule();

    AnilistAiringScheduleNode validNode = new AnilistAiringScheduleNode();
    validNode.setEpisode(1);
    validNode.setAiringAt(1712000000L);

    AnilistAiringScheduleNode nullEpisodeNode = new AnilistAiringScheduleNode();
    nullEpisodeNode.setEpisode(null);
    nullEpisodeNode.setAiringAt(1712000000L);

    AnilistAiringScheduleNode nullAiringAtNode = new AnilistAiringScheduleNode();
    nullAiringAtNode.setEpisode(2);
    nullAiringAtNode.setAiringAt(null);

    AnilistAiringScheduleNode doubleNullNode = new AnilistAiringScheduleNode();
    doubleNullNode.setEpisode(null);
    doubleNullNode.setAiringAt(null);

    schedule.setNodes(Arrays.asList(validNode, nullEpisodeNode, nullAiringAtNode, doubleNullNode));
    media.setAiringSchedule(schedule);

    var mapMethod = AnimeImportService.class.getDeclaredMethod("buildAirDateMap", AnilistMedia.class);
    mapMethod.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<Integer, LocalDate> result = (Map<Integer, LocalDate>) mapMethod.invoke(service, media);

    assertThat(result)
        .hasSize(1)
        .containsKey(1)
        .doesNotContainKey(2); // Проверяем, что нода с null временем не прошла
  }

  @ParameterizedTest(name = "isAcceptableFormat: {0} -> {1}")
  @MethodSource("provideFormatScenarios")
  void isAcceptableFormat_fullCoverage(String format, boolean expected) throws Exception {
    // Подготовка объекта
    AnilistMedia media = null;
    if (!"MEDIA_NULL".equals(format)) {
      media = new AnilistMedia();
      if (!"FORMAT_NULL".equals(format)) {
        media.setFormat(format);
      }
    }

    // Вызов приватного метода через рефлексию
    var method = AnimeImportService.class.getDeclaredMethod("isAcceptableFormat", AnilistMedia.class);
    method.setAccessible(true);

    boolean result = (boolean) method.invoke(service, media);

    // Проверка
    assertThat(result).isEqualTo(expected);
  }

  static Stream<Arguments> provideFormatScenarios() {
    return Stream.of(
        // Нулевые проверки (покрывают начальный if)
        Arguments.of("MEDIA_NULL", false),
        Arguments.of("FORMAT_NULL", false),

        Arguments.of("TV", true),
        Arguments.of("TV_SHORT", true), // Добавлено: 0 -> 1 хит
        Arguments.of("OVA", true),
        Arguments.of("ONA", true),      // Добавлено: 0 -> 1 хит
        Arguments.of("MOVIE", true),
        Arguments.of("SPECIAL", true),

        // Покрытие ветки default
        Arguments.of("MANGA", false),   // Любое значение не из списка
        Arguments.of("MUSIC", false)
    );
  }

  @Test
  @DisplayName("importFromApi — AniList возвращает null Media → Optional.empty()")
  void importFromApi_noMediaFound_returnsEmpty() throws Exception {
    String json = buildNullMediaJson();
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(json));

    Optional<Anime> result = service.importFromApi("NonExistentTitle");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("importFromApi — RestTemplate бросает исключение → Optional.empty()")
  void importFromApi_restTemplateThrows_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenThrow(new RuntimeException("Connection refused"));

    Optional<Anime> result = service.importFromApi("SomeTitle");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("importFromApi — тело ответа содержит errors + null Media → Optional.empty()")
  void importFromApi_anilistErrorsInBody_returnsEmpty() {
    String errorJson = """
        {
          "errors": [{ "message": "Not Found", "status": 404 }],
          "data": { "Media": null }
        }
        """;
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(errorJson));

    Optional<Anime> result = service.importFromApi("SomeTitle");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("refreshPopularAnime — Page.media не является массивом → ничего не обрабатывается")
  void refreshPopularAnime_notArrayResponse_nothingProcessed() {
    String json = """
        {
          "data": {
            "Page": {
              "media": "not_an_array"
            }
          }
        }
        """;
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(json));

    service.refreshPopularAnime(5);

    verify(animeRepository, never()).findByExternalId(any());
  }

  @Test
  @DisplayName("refreshPopularAnime — RestTemplate бросает исключение → метод не пробрасывает его")
  void refreshPopularAnime_restTemplateThrows_doesNotPropagate() {
    // GIVEN
    when(restTemplate.exchange(
        any(String.class),
        eq(HttpMethod.POST),
        any(),
        eq(String.class)
    )).thenThrow(new RuntimeException("Timeout"));

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
        service.refreshPopularAnime(10)
    );

    verify(restTemplate, times(1)).exchange(
        any(String.class),
        eq(HttpMethod.POST),
        any(),
        eq(String.class)
    );

    verify(animeRepository, never()).save(any());
  }

  @Test
  @DisplayName("refreshPopularAnime — пустой массив media → репозиторий не вызывается")
  void refreshPopularAnime_emptyMediaArray_nothingProcessed() {
    String json = """
        {
          "data": {
            "Page": {
              "media": []
            }
          }
        }
        """;
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(json));

    service.refreshPopularAnime(5);

    verify(animeRepository, never()).findByExternalId(any());
  }

  private String buildNullMediaJson() throws Exception {
    ObjectNode root = objectMapper.createObjectNode();
    ObjectNode data = objectMapper.createObjectNode();
    data.putNull("Media");
    root.set("data", data);
    return objectMapper.writeValueAsString(root);
  }

  @Test
  @DisplayName("refreshPopularAnimeWithProgress — успешный парсинг и импорт")
  void refreshPopularAnimeWithProgress_success() throws Exception {
    ImportTask task = new ImportTask("task-1", "Импорт", 0);

    // Используем константу PAGE_ONE_ITEM_JSON, которая уже есть в классе и корректно парсится
    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    // Мокаем вызов self.saveFranchise, чтобы не уходить в реальную работу с БД
    when(self.saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean())).thenReturn(new Anime());

    service.refreshPopularAnimeWithProgress(1, task);

    assertThat(task.getTotalCount()).isEqualTo(1);
    assertThat(task.getProcessedCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("refreshPopularAnimeWithProgress — если media не массив, бросает AnimeImportException")
  void refreshPopularAnimeWithProgress_notArray_throwsException() throws Exception {
    ImportTask task = new ImportTask("task-2", "Импорт", 0);

    // Передаем валидный JSON, но media — строка, а не массив
    String jsonResponse = "{\"data\":{\"Page\":{\"media\":\"not_an_array\"}}}";

    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jsonResponse));

    assertThatThrownBy(() -> service.refreshPopularAnimeWithProgress(1, task))
        .isInstanceOf(AnimeImportException.class)
        .hasMessageContaining("Failed to fetch or parse anime list");
  }

  @Test
  @DisplayName("refreshPopularAnimeWithProgress — JsonProcessingException логируется, цикл продолжается")
  void refreshPopularAnimeWithProgress_jsonProcessingException_continues() throws Exception {
    ImportTask task = new ImportTask("task-3", "Импорт", 0);

    // Делаем поле id объектом {}, хотя маппер ожидает число Long.
    // Это заставит реальный ObjectMapper выбросить MismatchedInputException (наследник JsonProcessingException)
    String invalidTypeJson = "{\"data\":{\"Page\":{\"media\":[{\"id\": {}}]}}}";

    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(invalidTypeJson));

    service.refreshPopularAnimeWithProgress(1, task);

    assertThat(task.getTotalCount()).isEqualTo(1);
    assertThat(task.getProcessedCount()).isZero(); // Не обработано из-за ошибки маппинга
  }

  @Test
  @DisplayName("refreshPopularAnimeWithProgress — InterruptedException бросает AnimeImportException")
  void refreshPopularAnimeWithProgress_interruptedException_throwsException() throws Exception {
    ImportTask task = new ImportTask("task-4", "Импорт", 0);

    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    // Имитируем прерывание потока внутри обработки одного франчайза
    when(self.saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean())).thenAnswer(inv -> {
      Thread.currentThread().interrupt();
      return new Anime();
    });

    assertThatThrownBy(() -> service.refreshPopularAnimeWithProgress(1, task))
        .isInstanceOf(AnimeImportException.class)
        .hasMessageContaining("Import process was interrupted");

    Thread.interrupted(); // Очищаем флаг прерывания, чтобы не сломать другие тесты
  }

  @Test
  @DisplayName("refreshPopularAnimeWithProgress — непредвиденная ошибка логируется, цикл идет дальше")
  void refreshPopularAnimeWithProgress_generalException_continues() throws Exception {
    ImportTask task = new ImportTask("task-5", "Импорт", 0);

    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    when(self.saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean()))
        .thenThrow(new RuntimeException("Test unexpected error"));

    service.refreshPopularAnimeWithProgress(1, task);

    assertThat(task.getTotalCount()).isEqualTo(1);
    assertThat(task.getProcessedCount()).isZero();
  }

  @Test
  @DisplayName("refreshOngoingAnime — успешное обновление онгоингов")
  void refreshOngoingAnime_success() throws Exception {
    when(animeRepository.findExternalIdsByIsOngoing(true)).thenReturn(List.of(101L));

    // Используем готовый валидный JSON
    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_WITH_EPS_JSON));

    when(self.saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean())).thenReturn(new Anime());

    service.refreshOngoingAnime();

    verify(animeRepository).findExternalIdsByIsOngoing(true);
    verify(self).saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean());
  }

  @Test
  @DisplayName("refreshOngoingAnime — прерывание потока корректно останавливает цикл")
  void refreshOngoingAnime_interrupted() throws Exception {
    when(animeRepository.findExternalIdsByIsOngoing(true)).thenReturn(List.of(111L, 222L));

    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          Thread.currentThread().interrupt();
          throw new RuntimeException("Simulate network error");
        });

    service.refreshOngoingAnime();

    verify(animeRepository).findExternalIdsByIsOngoing(true);
    Thread.interrupted();
  }
//
//  @Test
//  @DisplayName("refreshFinishedAnime — успешное обновление завершённых")
//  void refreshFinishedAnime_success() throws Exception {
//    when(animeRepository.findExternalIdsByIsOngoingFalseAndLastUpdatedBefore(any(LocalDateTime.class)))
//        .thenReturn(List.of(101L));
//
//    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
//        .thenReturn(ResponseEntity.ok(FINISHED_TV_WITH_EPS_JSON));
//
//    when(self.saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean())).thenReturn(new Anime());
//
//    service.refreshFinishedAnime();
//
//    verify(animeRepository).findExternalIdsByIsOngoingFalseAndLastUpdatedBefore(any(LocalDateTime.class));
//    verify(self).saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean());
//  }
//
//  @Test
//  @DisplayName("refreshFinishedAnime — прерывание потока корректно останавливает цикл")
//  void refreshFinishedAnime_interrupted() throws Exception {
//    when(animeRepository.findExternalIdsByIsOngoingFalseAndLastUpdatedBefore(any(LocalDateTime.class)))
//        .thenReturn(List.of(333L));
//
//    when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
//        .thenAnswer(inv -> {
//          Thread.currentThread().interrupt();
//          throw new RuntimeException("Simulate network error");
//        });
//
//    service.refreshFinishedAnime();
//
//    verify(animeRepository).findExternalIdsByIsOngoingFalseAndLastUpdatedBefore(any(LocalDateTime.class));
//    Thread.interrupted();
//  }

  private AnilistMedia tvMedia(long id, String status, Integer episodes) {
    AnilistMedia m = new AnilistMedia();
    m.setId(id);
    AnilistTitle title = new AnilistTitle();
    title.setRomaji("TestAnime-" + id);
    m.setTitle(title);
    m.setFormat("TV");
    m.setStatus(status);
    m.setEpisodes(episodes);
    return m;
  }

  private AnilistMedia tvMediaWithStartDate(long id, String status) {
    AnilistMedia m = tvMedia(id, status, null);
    AnilistDate date = new AnilistDate();
    date.setYear(2025);
    date.setMonth(4);
    date.setDay(1);
    m.setStartDate(date);
    return m;
  }

  private void addSequelEdges(AnilistMedia media, Long... relatedIds) {
    List<AnilistRelationEdge> edges = new java.util.ArrayList<>();
    for (Long relId : relatedIds) {
      AnilistMedia relNode = new AnilistMedia();
      relNode.setId(relId);

      AnilistRelationEdge edge = new AnilistRelationEdge();
      edge.setRelationType("SEQUEL");
      edge.setNode(relNode);
      edges.add(edge);
    }
    AnilistRelations relations = new AnilistRelations();
    relations.setEdges(edges);
    media.setRelations(relations);
  }

  private void mockSaveFranchise(String animeTitle) {
    Anime anime = new Anime();
    anime.setTitle(animeTitle);
    when(self.saveFranchise(any(), anyInt(), anyBoolean(), anyBoolean()))
        .thenReturn(anime);
  }

  @Test
  void processFranchise_tvReleasingStatus_isOngoingTrue() {
    mockSaveFranchise("TestAnime");
    AnilistMedia media = tvMedia(1L, "RELEASING", 12);

    Anime result = service.processFranchise(media);

    assertThat(result).isNotNull();
    verify(self).saveFranchise(any(), anyInt(), eq(true), anyBoolean());
  }

  @Test
  void processFranchise_notYetReleasedNoDateButHasEpisodes_isOngoingTrue() {
    mockSaveFranchise("TestAnime");
    AnilistMedia media = tvMedia(2L, "NOT_YET_RELEASED", 12);

    Anime result = service.processFranchise(media);

    assertThat(result).isNotNull();
    verify(self).saveFranchise(any(), anyInt(), eq(true), anyBoolean());
  }

  @Test
  void processFranchise_notYetReleasedNoDateNullEpisodes_isOngoingFalse() {
    mockSaveFranchise("TestAnime");
    AnilistMedia media = tvMedia(3L, "NOT_YET_RELEASED", null);

    Anime result = service.processFranchise(media);

    assertThat(result).isNotNull();
    verify(self).saveFranchise(any(), anyInt(), eq(false), anyBoolean());
  }

  @Test
  @DisplayName("processFranchise — завершенное аниме НЕ считается онгоингом")
  void processFranchise_finishedWithEpisodes_isOngoingFalse() {
    mockSaveFranchise("TestAnime-5");

    AnilistMedia media = tvMedia(5L, "FINISHED", 12);

    Anime result = service.processFranchise(media);

    assertThat(result).isNotNull();

    verify(self).saveFranchise(any(), anyInt(), eq(false), eq(false));
  }

  @Test
  @DisplayName("processFranchise — NOT_YET_RELEASED с эпизодами считается онгоингом и анонсом")
  void processFranchise_notYetReleasedWithEpisodes_isOngoingTrue() {
    mockSaveFranchise("TestAnime-4");

    AnilistMedia media = tvMedia(4L, "NOT_YET_RELEASED", 12);
    media.setStartDate(null);

    Anime result = service.processFranchise(media);

    assertThat(result).isNotNull();

    verify(self).saveFranchise(
        any(),
        eq(0),     // tvCount = 0, т.к. NOT_YET_RELEASED фильтруется в tvCount
        eq(true),  // isOngoing = true
        eq(true)   // isAnnounced = true
    );
  }

  @Test
  @DisplayName("processFranchise — покрытие условия: статус NOT_YET_RELEASED, даты нет, эпизоды > 0")
  void processFranchise_notYetReleasedNoDateWithEpisodes_isOngoingTrue() {
    mockSaveFranchise("Test-Ongoing-Logic");

    AnilistMedia media = tvMedia(999L, "NOT_YET_RELEASED", 12);

    media.setStartDate(null);

    Anime result = service.processFranchise(media);

    assertThat(result).isNotNull();

    verify(self).saveFranchise(
        anyList(),
        eq(0),     // tvCount
        eq(true),  // isOngoing
        eq(true)   // isAnnounced
    );
  }

  @Test
  @DisplayName("processFranchise — покрытие ветки RELEASING (return true)")
  void processFranchise_releasingStatus_isOngoingTrue() {
    mockSaveFranchise("Test-Releasing");

    AnilistMedia media = tvMedia(888L, "RELEASING", 24);

    Anime result = service.processFranchise(media);

    assertThat(result).isNotNull();

    verify(self).saveFranchise(anyList(), eq(1), eq(true), eq(true));
  }

  @Test
  void processFranchise_notYetReleasedWithStartDate_isOngoingTrue() {
    mockSaveFranchise("TestAnime");
    AnilistMedia media = tvMediaWithStartDate(5L, "NOT_YET_RELEASED");

    Anime result = service.processFranchise(media);

    assertThat(result).isNotNull();
    verify(self).saveFranchise(any(), anyInt(), eq(true), anyBoolean());
  }

  @Test
  void registerNode_duplicateRelIdInEdges_addedToFetchOnlyOnce() {
    mockSaveFranchise("TestAnime");

    AnilistMedia media = tvMedia(10L, "FINISHED", 12);
    addSequelEdges(media, 99L, 99L); // один и тот же id дважды

    AnimeImportService spyService = spy(service);
    doReturn(null).when(spyService).fetchAnilistByIdWithRetry(99L);

    Anime result = spyService.processFranchise(media);

    assertThat(result).isNotNull();
    verify(spyService, org.mockito.Mockito.times(1)).fetchAnilistByIdWithRetry(99L);
  }

  @Test
  void refreshOngoingAnime_whenFetchThrowsException_logsErrorAndContinues() {
    when(animeRepository.findExternalIdsByIsOngoing(true)).thenReturn(List.of(123L));

    AnimeImportService spyService = spy(service);
    doThrow(new RuntimeException("Anilist unavailable"))
        .when(spyService).fetchAnilistByIdWithRetry(123L);

    org.assertj.core.api.Assertions.assertThatNoException()
        .isThrownBy(spyService::refreshOngoingAnime);

    verify(spyService).fetchAnilistByIdWithRetry(123L);
  }

  @Test
  void refreshOngoingAnime_whenFetchThrowsException_processesAllIdsInList() {
    when(animeRepository.findExternalIdsByIsOngoing(true))
        .thenReturn(List.of(101L, 102L));

    AnimeImportService spyService = spy(service);
    doThrow(new RuntimeException("error")).when(spyService).fetchAnilistByIdWithRetry(101L);
    doThrow(new RuntimeException("error")).when(spyService).fetchAnilistByIdWithRetry(102L);

    org.assertj.core.api.Assertions.assertThatNoException()
        .isThrownBy(spyService::refreshOngoingAnime);

    verify(spyService).fetchAnilistByIdWithRetry(101L);
    verify(spyService).fetchAnilistByIdWithRetry(102L);
  }
//
//  @Test
//  void refreshFinishedAnime_whenFetchThrowsException_logsErrorAndContinues() {
//    when(animeRepository.findExternalIdsByIsOngoingFalseAndLastUpdatedBefore(any(LocalDateTime.class)))
//        .thenReturn(List.of(456L));
//
//    AnimeImportService spyService = spy(service);
//    doThrow(new RuntimeException("DB connection lost"))
//        .when(spyService).fetchAnilistByIdWithRetry(456L);
//
//    org.assertj.core.api.Assertions.assertThatNoException()
//        .isThrownBy(spyService::refreshFinishedAnime);
//
//    verify(spyService).fetchAnilistByIdWithRetry(456L);
//  }

//  @Test
//  void refreshFinishedAnime_whenFetchThrowsException_processesAllIdsInList() {
//    when(animeRepository.findExternalIdsByIsOngoingFalseAndLastUpdatedBefore(any(LocalDateTime.class)))
//        .thenReturn(List.of(201L, 202L));
//
//    AnimeImportService spyService = spy(service);
//    doThrow(new RuntimeException("error")).when(spyService).fetchAnilistByIdWithRetry(201L);
//    doThrow(new RuntimeException("error")).when(spyService).fetchAnilistByIdWithRetry(202L);
//
//    org.assertj.core.api.Assertions.assertThatNoException()
//        .isThrownBy(spyService::refreshFinishedAnime);
//
//    verify(spyService).fetchAnilistByIdWithRetry(201L);
//    verify(spyService).fetchAnilistByIdWithRetry(202L);
//  }

  @Test
  void refreshOngoingAnime_whenEmptyList_doesNotCallFetch() {
    when(animeRepository.findExternalIdsByIsOngoing(true)).thenReturn(List.of());

    AnimeImportService spyService = spy(service);
    spyService.refreshOngoingAnime();

    verify(spyService, never()).fetchAnilistByIdWithRetry(anyLong());
  }

//  @Test
//  void refreshFinishedAnime_whenEmptyList_doesNotCallFetch() {
//    when(animeRepository.findExternalIdsByIsOngoingFalseAndLastUpdatedBefore(any(LocalDateTime.class)))
//        .thenReturn(List.of());
//
//    AnimeImportService spyService = spy(service);
//    spyService.refreshFinishedAnime();
//
//    verify(spyService, never()).fetchAnilistByIdWithRetry(anyLong());
//  }

  @Test
  @DisplayName("isOngoing branch coverage: NOT_YET_RELEASED, date is null, episodes > 0 -> true")
  void isOngoing_NotYetReleased_NoDate_WithEpisodes_ReturnsTrue() {
    mockSaveFranchise("Coverage-1");
    AnilistMedia media = tvMedia(101L, "NOT_YET_RELEASED", 12);
    media.setStartDate(null);

    service.processFranchise(media);

    verify(self).saveFranchise(any(), anyInt(), eq(true), anyBoolean());
  }

  @Test
  @DisplayName("NOT_YET_RELEASED + no date + episodes=0 → isOngoing=false (line coverage)")
  void isOngoing_notYetReleased_noDate_zeroEpisodes_returnsFalse() {
    mockSaveFranchise("Coverage-4");

    AnilistMedia media = tvMedia(1L, "NOT_YET_RELEASED", 0);

    AnimeImportService spyService = spy(service);
    doReturn(new ArrayList<>(List.of(media))).when(spyService).collectFullChain(any());

    spyService.processFranchise(media);

    verify(self).saveFranchise(any(), anyInt(), eq(false), anyBoolean());
  }

  @Test
  @DisplayName("isOngoing branch coverage: NOT_YET_RELEASED, date is null, episodes is null -> false")
  void isOngoing_NotYetReleased_NoDate_NullEpisodes_ReturnsFalse() {
    mockSaveFranchise("Coverage-3");
    AnilistMedia media = tvMedia(103L, "NOT_YET_RELEASED", 1); // создаем с 1
    media.setEpisodes(null); // принудительно зануляем
    media.setStartDate(null);

    service.processFranchise(media);

    verify(self).saveFranchise(any(), anyInt(), eq(false), anyBoolean());
  }

  // ─── helpers ────────────────────────────────────────────────────────────────

  private org.example.animetracker.dto.external.AnilistMedia parseMedia(String json)
      throws Exception {
    var node = objectMapper.readTree(json).path("data").path("Media");
    return objectMapper.treeToValue(
        node, org.example.animetracker.dto.external.AnilistMedia.class);
  }

  private Anime buildSavedAnime(Long id, String title) {
    Anime a = new Anime();
    a.setId(id);
    a.setTitle(title);
    return a;
  }

  private Season buildSavedSeason(Long id) {
    Season s = new Season();
    s.setId(id);
    return s;
  }
}