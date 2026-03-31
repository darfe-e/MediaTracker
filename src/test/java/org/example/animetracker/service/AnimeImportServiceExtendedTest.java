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
import org.example.animetracker.cache.AnimeSearchCache;
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