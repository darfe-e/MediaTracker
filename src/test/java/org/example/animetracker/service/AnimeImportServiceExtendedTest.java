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

  // ─── общий JSON для AnilistMedia ─────────────────────────────────────────
  private static final String FINISHED_TV_MEDIA_JSON = """
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

  private static final String RELEASING_MEDIA_WITH_SCHEDULE_JSON = """
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

  private static final String UNKNOWN_FORMAT_MEDIA_JSON = """
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

  private static final String PAGE_WITH_ONE_ITEM_JSON = """
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
              "episodes": 0,
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
    var media = objectMapper.readTree(FINISHED_TV_MEDIA_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        media, org.example.animetracker.dto.external.AnilistMedia.class);

    Anime savedAnime = new Anime();
    savedAnime.setId(1L);
    savedAnime.setTitle("Test Anime EN");

    Season savedSeason = new Season();
    savedSeason.setId(10L);

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
    var media = objectMapper.readTree(FINISHED_TV_MEDIA_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        media, org.example.animetracker.dto.external.AnilistMedia.class);

    Anime existing = new Anime();
    existing.setId(99L);
    existing.setTitle("Old Title");

    Season savedSeason = new Season();
    savedSeason.setId(5L);

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
  @DisplayName("saveFranchise — формат не TV, нет TV-медиа → берёт первый элемент")
  void saveFranchise_noTvFormat_takesFirstElement() throws Exception {
    var mediaNode = objectMapper.readTree(UNKNOWN_FORMAT_MEDIA_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        mediaNode, org.example.animetracker.dto.external.AnilistMedia.class);

    Anime savedAnime = new Anime();
    savedAnime.setId(1L);

    Season savedSeason = new Season();
    savedSeason.setId(1L);

    when(animeRepository.findByExternalId(303L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(303L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    Anime result = service.saveFranchise(List.of(anilistMedia), 0, false);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("saveFranchise — RELEASING с расписанием → resolveEpisodeCount через airingSchedule")
  void saveFranchise_releasingWithSchedule_resolvesEpisodeCount() throws Exception {
    var mediaNode = objectMapper.readTree(RELEASING_MEDIA_WITH_SCHEDULE_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        mediaNode, org.example.animetracker.dto.external.AnilistMedia.class);

    Anime savedAnime = new Anime();
    savedAnime.setId(2L);

    Season savedSeason = new Season();
    savedSeason.setId(20L);

    // idMal=null → fetchJikanEpisodeTitles возвращает empty map без HTTP-вызовов
    when(animeRepository.findByExternalId(202L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(202L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, true);

    assertThat(result).isNotNull();
    // totalEps = maxEpisodeFromSchedule = 6 → эпизоды сохраняются
    verify(episodeRepository).saveAll(any());
    // buildAirDateMap тоже покрыт (2 узла с airingAt)
  }

  @Test
  @DisplayName("saveFranchise — сезон уже существует в БД → обновляется")
  void saveFranchise_existingSeason_updates() throws Exception {
    var mediaNode = objectMapper.readTree(FINISHED_TV_MEDIA_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        mediaNode, org.example.animetracker.dto.external.AnilistMedia.class);

    Anime savedAnime = new Anime();
    savedAnime.setId(1L);

    Season existingSeason = new Season();
    existingSeason.setId(77L);
    existingSeason.setExternalId(101L);

    when(animeRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(genreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(101L)).thenReturn(Optional.of(existingSeason));
    when(seasonRepository.save(any())).thenReturn(existingSeason);

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

    assertThat(result).isNotNull();
    verify(seasonRepository).save(existingSeason);
  }

  @Test
  @DisplayName("saveFranchise — жанры сохраняются через genreRepository.save")
  void saveFranchise_withNewGenres_savesGenresToRepository() throws Exception {
    var mediaNode = objectMapper.readTree(FINISHED_TV_MEDIA_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        mediaNode, org.example.animetracker.dto.external.AnilistMedia.class);
    // Медиа содержит жанры ["Action", "Adventure"], genreCache пуст →
    // оба жанра сохранятся через genreRepository.save

    Anime savedAnime = new Anime();
    savedAnime.setId(1L);
    Season savedSeason = new Season();
    savedSeason.setId(10L);

    when(animeRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of()); // пустой кэш жанров
    when(genreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(101L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);

    service.saveFranchise(List.of(anilistMedia), 1, false);

    // 2 новых жанра → genreRepository.save вызывается 2 раза
    verify(genreRepository, times(2)).save(any());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // importFromApi — успешный путь через processFranchise → self.saveFranchise
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("importFromApi — медиа найдена, self.saveFranchise возвращает Anime → present")
  void importFromApi_validMedia_saveFranchiseReturnsAnime_returnsPresent() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_MEDIA_JSON));

    Anime expectedAnime = new Anime();
    expectedAnime.setId(1L);
    expectedAnime.setTitle("Test Anime EN");
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenReturn(expectedAnime);

    Optional<Anime> result = service.importFromApi("Test Anime");

    assertThat(result).isPresent();
    assertThat(result.get().getTitle()).isEqualTo("Test Anime EN");
  }

  @Test
  @DisplayName("importFromApi — медиа найдена, self.saveFranchise выбрасывает → Optional.empty")
  void importFromApi_validMedia_saveFranchiseThrows_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_MEDIA_JSON));

    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenThrow(new RuntimeException("DB error"));

    Optional<Anime> result = service.importFromApi("Test Anime");

    // исключение перехватывается catch-блоком importFromApi → empty
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("importFromApi — медиа формата MUSIC (неприемлемый) → allMedia пуст → empty")
  void importFromApi_unacceptableFormat_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(UNKNOWN_FORMAT_MEDIA_JSON));

    Optional<Anime> result = service.importFromApi("Music Video");

    // MUSIC — не в acceptable formats → allMedia пуст → processFranchise null
    assertThat(result).isEmpty();
    verify(self, never()).saveFranchise(any(), any(int.class), any(boolean.class));
  }

  @Test
  @DisplayName("importFromApi — self.saveFranchise возвращает null → Optional.empty")
  void importFromApi_saveFranchiseReturnsNull_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(FINISHED_TV_MEDIA_JSON));

    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenReturn(null);

    Optional<Anime> result = service.importFromApi("Test Anime");

    assertThat(result).isEmpty();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // refreshPopularAnime — путь с медиа-элементами в массиве
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("refreshPopularAnime — массив с одним элементом → processMediaNode вызывается")
  void refreshPopularAnime_oneItem_processesNode() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_WITH_ONE_ITEM_JSON));

    Anime anime = new Anime();
    anime.setId(1L);
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenReturn(anime);

    // Не бросает исключений, несмотря на Thread.sleep(3000) в processMediaNode
    service.refreshPopularAnime(1);

    verify(self).saveFranchise(any(), any(int.class), any(boolean.class));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // executeAnilistQuery — ответ не 2xx → выбрасывает IllegalStateException
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("importFromApi — AniList вернул 500 → executeAnilistQuery бросает → empty")
  void importFromApi_anilistHttpError_returnsEmpty() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("error"));

    Optional<Anime> result = service.importFromApi("SomeTitle");

    // IllegalStateException поймано в fetchAnilistByTitle → null → Optional.empty
    assertThat(result).isEmpty();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // refreshPopularAnime — processMediaNode: InterruptedException
  // ═══════════════════════════════════════════════════════════════════════════

  @Test
  @DisplayName("refreshPopularAnime — processMediaNode ловит исключение, не пробрасывает")
  void refreshPopularAnime_processingThrows_doesNotPropagate() {
    when(restTemplate.exchange(
        any(String.class), eq(HttpMethod.POST), any(), eq(String.class)))
        .thenReturn(ResponseEntity.ok(PAGE_WITH_ONE_ITEM_JSON));

    // self.saveFranchise выбрасывает → processMediaNode ловит его как Exception
    when(self.saveFranchise(any(), any(int.class), any(boolean.class)))
        .thenThrow(new RuntimeException("processing error"));

    // Метод НЕ должен пробрасывать исключение
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> service.refreshPopularAnime(1)
    );
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Jikan: idMal != null → fetchJikanPage вызывается (Thread.sleep(400))
  // ═══════════════════════════════════════════════════════════════════════════

  private static final String MEDIA_WITH_MAL_ID_JSON = """
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

  @Test
  @DisplayName("saveFranchise — idMal != null, Jikan возвращает эпизоды с title")
  void saveFranchise_withMalId_jikanReturnsEpisodes() throws Exception {
    var mediaNode = objectMapper.readTree(MEDIA_WITH_MAL_ID_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        mediaNode, org.example.animetracker.dto.external.AnilistMedia.class);

    String jikanResponse = """
        {
          "data": [
            {"mal_id": 1, "title": "Episode 1 Title", "title_romanji": null},
            {"mal_id": 2, "title": null, "title_romanji": "Ep 2 Romanji"}
          ],
          "pagination": {"has_next_page": false}
        }
        """;

    Anime savedAnime = new Anime();
    savedAnime.setId(1L);
    Season savedSeason = new Season();
    savedSeason.setId(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    // Jikan HTTP call через restTemplate.getForEntity
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jikanResponse));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

    assertThat(result).isNotNull();
    verify(episodeRepository).saveAll(any());
    verify(restTemplate).getForEntity(any(String.class), eq(String.class));
  }

  @Test
  @DisplayName("saveFranchise — Jikan возвращает пустой data-массив → нет следующей страницы")
  void saveFranchise_jikanEmptyData_returnsNoMorePages() throws Exception {
    var mediaNode = objectMapper.readTree(MEDIA_WITH_MAL_ID_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        mediaNode, org.example.animetracker.dto.external.AnilistMedia.class);

    String jikanEmptyResponse = """
        {
          "data": [],
          "pagination": {"has_next_page": false}
        }
        """;

    Anime savedAnime = new Anime();
    savedAnime.setId(1L);
    Season savedSeason = new Season();
    savedSeason.setId(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.ok(jikanEmptyResponse));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);

    assertThat(result).isNotNull();
    // Пустой data → fetchJikanPage вернул false → заголовки по умолчанию
    verify(episodeRepository).saveAll(any());
  }

  @Test
  @DisplayName("saveFranchise — Jikan возвращает не-2xx → fetchJikanPage возвращает false")
  void saveFranchise_jikanNon2xx_handledGracefully() throws Exception {
    var mediaNode = objectMapper.readTree(MEDIA_WITH_MAL_ID_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        mediaNode, org.example.animetracker.dto.external.AnilistMedia.class);

    Anime savedAnime = new Anime();
    savedAnime.setId(1L);
    Season savedSeason = new Season();
    savedSeason.setId(10L);

    when(animeRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(genreRepository.findAll()).thenReturn(List.of());
    when(animeRepository.save(any())).thenReturn(savedAnime);
    when(seasonRepository.findByExternalId(404L)).thenReturn(Optional.empty());
    when(seasonRepository.save(any())).thenReturn(savedSeason);
    when(restTemplate.getForEntity(any(String.class), eq(String.class)))
        .thenReturn(ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(null));
    when(episodeRepository.saveAll(any())).thenReturn(List.of());

    // Не пробрасывает исключение
    Anime result = service.saveFranchise(List.of(anilistMedia), 1, false);
    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("saveFranchise — Jikan выбрасывает исключение → fetchJikanEpisodeTitles ловит его")
  void saveFranchise_jikanThrowsException_handledGracefully() throws Exception {
    var mediaNode = objectMapper.readTree(MEDIA_WITH_MAL_ID_JSON)
        .path("data").path("Media");
    var anilistMedia = objectMapper.treeToValue(
        mediaNode, org.example.animetracker.dto.external.AnilistMedia.class);

    Anime savedAnime = new Anime();
    savedAnime.setId(1L);
    Season savedSeason = new Season();
    savedSeason.setId(10L);

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
    // Эпизоды сохраняются с заголовками по умолчанию "Episode N"
    verify(episodeRepository).saveAll(any());
  }
}