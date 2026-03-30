package org.example.mediatracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Optional;
import org.example.animetracker.cache.AnimeSearchCache;
import org.example.animetracker.model.Anime;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.EpisodeRepository;
import org.example.animetracker.repository.GenreRepository;
import org.example.animetracker.repository.SeasonRepository;
import org.example.animetracker.service.AnimeImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
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
  @Mock private AnimeImportService self;   // @Lazy self-injection

  private ObjectMapper objectMapper;
  private AnimeImportService service;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new AnimeImportService(
        animeRepository, genreRepository, seasonRepository,
        episodeRepository, restTemplate, objectMapper, self, searchCache);
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
}