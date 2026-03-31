package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.example.animetracker.cache.AnimeSearchCache;
import org.example.animetracker.dto.external.AnilistMedia;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Season;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.EpisodeRepository;
import org.example.animetracker.repository.GenreRepository;
import org.example.animetracker.repository.SeasonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Расширенные тесты AnimeImportService. Класс находится в том же пакете,
 * что и сервис, для доступа к protected-методу saveFranchise.
 */
@ExtendWith(MockitoExtension.class)
class AnimeImportServiceExtendedTest {

  @Mock private AnimeRepository    animeRepository;
  @Mock private GenreRepository    genreRepository;
  @Mock private SeasonRepository   seasonRepository;
  @Mock private EpisodeRepository  episodeRepository;
  @Mock private RestTemplate       restTemplate;
  @Mock private AnimeSearchCache   searchCache;
  @Mock private AnimeImportService self;

  private ObjectMapper objectMapper;
  private AnimeImportService service;

  // ─── JSON-константы ──────────────────────────────────────────────────────

  /**
   * episodes=0 → hasEpisodes() == false → registerNode НЕ добавит медиа в allMedia.
   * Используется только в тестах, вызывающих saveFranchise() напрямую.
   */
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

  // ═══════════════════════════════════════════════════════════════════════════
  // saveFranchise — вызывается напрямую (protected, тот же пакет)
  // ═══════════════════════════════════════════════════════════════════════════

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Test Anime EN");
    verify(animeRepository).save(any());
    verify(searchCache).invalidateAll();
    // episodes=0 → saveAll никогда не вызывается
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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 0, false);

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, true);

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

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

    service.saveFranchise(List.of(anilistMedia), 1, false);

    // 2 новых жанра → save вызывается 2 раза
    verify(genreRepository, times(2)).save(any());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // importFromApi — через processFranchise → self.saveFranchise
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("importFromApi — медиа найдена (episodes=12), saveFranchise → present")
  void importFromApi_validMedia_saveFranchiseReturnsAnime_returnsPresent() {
    // episodes=12 → hasEpisodes() == true → registerNode добавляет в allMedia
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_WITH_EPS_JSON));

    Anime expected = buildSavedAnime(1L, "Test Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
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

    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
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

    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
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
    verify(self, never()).saveFranchise(any(), any(int.class), any(boolean.class));
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

  // ═══════════════════════════════════════════════════════════════════════════
  // refreshPopularAnime
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("refreshPopularAnime — один элемент (episodes=12) → self.saveFranchise вызывается")
  void refreshPopularAnime_oneItem_processesNode() {
    // episodes=12 → hasEpisodes() == true → медиа попадает в allMedia
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    Anime anime = buildSavedAnime(1L, "Test Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenReturn(anime);

    service.refreshPopularAnime(1);

    verify(self).saveFranchise(any(), any(int.class), any(boolean.class));
  }

  @Test
  @DisplayName("refreshPopularAnime — processMediaNode ловит исключение, не пробрасывает")
  void refreshPopularAnime_processingThrows_doesNotPropagate() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    // saveFranchise выбрасывает → processMediaNode ловит как Exception
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenThrow(new RuntimeException("processing error"));

    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> service.refreshPopularAnime(1)
    );
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // processMediaNode — InterruptedException (Thread.sleep(3000))
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("refreshPopularAnime — processMediaNode: Thread.sleep(3000) прерван → IllegalState перехватывается")
  void refreshPopularAnime_processMediaNodeInterrupted_doesNotPropagate() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_ONE_ITEM_JSON));

    // Прерываем поток внутри saveFranchise — Thread.sleep(3000) после него кинет
    // InterruptedException немедленно (0 мс). processMediaNode поймает и пробросит
    // как IllegalStateException, которое поймает refreshPopularAnime.
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
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



  // ═══════════════════════════════════════════════════════════════════════════
  // collectFullChain — обход SEQUEL-связи, связанное медиа найдено
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("importFromApi — SEQUEL-связь: связанная запись найдена → allMedia содержит 2 элемента")
  void importFromApi_sequelRelation_relatedMediaFound() {
    // Вызов 1 (fetchAnilistByTitle): стартовое медиа с SEQUEL на 501
    // Вызов 2 (fetchAnilistByIdWithRetry для 501): сиквел с обратной PREQUEL → visited.contains(500)=true
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
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    assertThat(result).isPresent();
    // saveFranchise вызван ровно один раз с assembled franchise
    verify(self).saveFranchise(any(), any(int.class), any(boolean.class));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // collectFullChain — обход SEQUEL-связи, связанное медиа не найдено (null)
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("importFromApi — SEQUEL-связь: связанная запись null → log.warn, продолжает с основным")
  void importFromApi_sequelRelation_relatedMediaNull_logWarn() {
    // {"data":{"Media":null}} → fetchAnilistByIdWithRetry возвращает null сразу (без ретраев)
    // → log.warn("Не удалось загрузить id=501 (пропускаем)")
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
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    assertThat(result).isPresent();
    // Только стартовая запись в allMedia, saveFranchise вызван
    verify(self).saveFranchise(any(), any(int.class), any(boolean.class));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // fetchAnilistByIdWithRetry — retry sleep прерван → InterruptedException → null
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("importFromApi — retry-sleep прерывается → fetchAnilistByIdWithRetry возвращает null")
  void importFromApi_fetchAnilistByIdRetry_interruptedDuringSleep_returnsNull() {
    // Вызов 1: стартовое медиа с SEQUEL на 501
    // Вызов 2 (attempt=1 для 501): прерываем поток + кидаем исключение →
    AtomicInteger callCount = new AtomicInteger(0);
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenAnswer(inv -> {
          int call = callCount.incrementAndGet();
          if (call == 1) {
            return ResponseEntity.ok(MEDIA_WITH_SEQUEL_JSON);
          }
          // Устанавливаем флаг до броска → Thread.sleep(2000) в retry сработает немедленно
          Thread.currentThread().interrupt();
          throw new RuntimeException("network error");
        });

    Anime anime = buildSavedAnime(1L, "Start Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Start Anime");

    Thread.interrupted();

    // Связанная запись не загружена, но основная цепочка обработана
    assertThat(result).isPresent();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // importFromApi — episodes=0 → hasEpisodes() false → allMedia пуст → empty
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("importFromApi — episodes=0 → hasEpisodes false → allMedia пуст → Optional.empty")
  void importFromApi_episodesZero_hasEpisodesFalse_returnsEmpty() {
    // FINISHED_TV_NO_EPS_JSON: format=TV (isAcceptableFormat=true), episodes=0 (hasEpisodes=false)
    // → registerNode НЕ добавляет в result → allMedia пуст → processFranchise возвращает null
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_NO_EPS_JSON));

    Optional<Anime> result = service.importFromApi("Test Anime");

    assertThat(result).isEmpty();
    verify(self, never()).saveFranchise(any(), any(int.class), any(boolean.class));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // isAcceptableFormat — OVA формат через importFromApi (registerNode)
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("importFromApi — формат OVA → isAcceptableFormat true → входит в allMedia")
  void importFromApi_ovaFormat_isAcceptableFormat() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(OVA_WITH_EPS_JSON));

    Anime anime = buildSavedAnime(1L, "Test OVA");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenReturn(anime);

    Optional<Anime> result = service.importFromApi("Test OVA");

    assertThat(result).isPresent();
    // OVA → isAcceptableFormat=true → allMedia содержит медиа → saveFranchise вызван
    verify(self).saveFranchise(any(), any(int.class), any(boolean.class));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // isTvFormat — формат TV_SHORT
  // ═══════════════════════════════════════════════════════════════════════════

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

    assertThat(result).isNotNull();
    verify(animeRepository).save(any());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // isReleasingOrUpcoming — статус NOT_YET_RELEASED
  // ═══════════════════════════════════════════════════════════════════════════

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 0, true);

    assertThat(result).isNotNull();
    // totalEps = maxEpisodeFromSchedule = 1 → saveAll вызывается
    verify(episodeRepository).saveAll(any());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Jikan: idMal != null → fetchJikanPage вызывается
  // ═══════════════════════════════════════════════════════════════════════════

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

    assertThat(result).isNotNull();
    // Эпизоды сохраняются с заголовками "Episode N" (по умолчанию)
    verify(episodeRepository).saveAll(any());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // fetchJikanEpisodeTitles — InterruptedException (Thread.sleep(400))
  // ═══════════════════════════════════════════════════════════════════════════

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

    // Прерываем поток ДО вызова saveFranchise.
    // Thread.sleep(400) в fetchJikanPage сработает немедленно (0 мс).
    // fetchJikanEpisodeTitles поймает InterruptedException, установит hasMore=false.
    Thread.currentThread().interrupt();

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

    Thread.interrupted();

    assertThat(result).isNotNull();
    // getForEntity не вызывается — sleep упал раньше
    verify(restTemplate, never()).getForEntity(any(String.class), eq(String.class));
    // Эпизоды всё равно сохраняются с дефолтными заголовками
    verify(episodeRepository).saveAll(any());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // fetchJikanEpisodeTitles — has_next_page=true → несколько страниц
  // ═══════════════════════════════════════════════════════════════════════════

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

    assertThat(result).isNotNull();
    // page=1 (has_next_page=true) + page=2 (has_next_page=false) → 2 запроса
    verify(restTemplate, times(2)).getForEntity(any(String.class), eq(String.class));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // fillAnimeInfo — null/пустой жанр в списке → ветка continue
  // ═══════════════════════════════════════════════════════════════════════════

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

    Anime result = service.saveFranchise(List.of(anilistMedia), 0, false);

    assertThat(result).isNotNull();
    // Только "Action" не пустой → genreRepository.save вызван ровно 1 раз
    verify(genreRepository, times(1)).save(any());
    // episodes=0 → saveAll не вызывается
    verify(episodeRepository, never()).saveAll(any());
  }

  @Test
  @DisplayName("processFranchise - возвращает null")
  void processFranchise_returns_null(){
    AnilistMedia startNode = null;

    Anime result = service.processFranchise(startNode);
    assertThat(result).isNull();
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