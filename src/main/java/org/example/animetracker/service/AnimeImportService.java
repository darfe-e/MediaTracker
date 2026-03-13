package org.example.animetracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.cache.AnimeSearchCache;
import org.example.animetracker.dto.external.AnilistAiringScheduleNode;
import org.example.animetracker.dto.external.AnilistMedia;
import org.example.animetracker.dto.external.AnilistRelationEdge;
import org.example.animetracker.dto.external.AnilistStudioEdge;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Episode;
import org.example.animetracker.model.Genre;
import org.example.animetracker.model.Season;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.EpisodeRepository;
import org.example.animetracker.repository.GenreRepository;
import org.example.animetracker.repository.SeasonRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class AnimeImportService {

  private final AnimeRepository animeRepository;
  private final GenreRepository genreRepository;
  private final SeasonRepository seasonRepository;
  private final EpisodeRepository episodeRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final AnimeSearchCache searchCache;

  private static final String ANILIST_API_URL = "https://graphql.anilist.co";
  private static final String JIKAN_API_URL   = "https://api.jikan.moe/v4";

  private static final String MEDIA_FIELDS =
            """
            id idMal popularity
            title { romaji english }
            format status episodes duration
            startDate { year month day }
            studios(isMain: true) { edges { node { name } } }
            genres
            airingSchedule(notYetAired: true) { nodes { airingAt episode } }
            relations {
                edges {
                    relationType
                    node { id }
                }
            }
            """;

  private final AnimeImportService self;

  public AnimeImportService(
      AnimeRepository animeRepository,
      GenreRepository genreRepository,
      SeasonRepository seasonRepository,
      EpisodeRepository episodeRepository,
      RestTemplate restTemplate,
      ObjectMapper objectMapper,
      @Lazy AnimeImportService self,
      AnimeSearchCache searchCache) {
    this.animeRepository = animeRepository;
    this.genreRepository = genreRepository;
    this.seasonRepository = seasonRepository;
    this.episodeRepository = episodeRepository;
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.self = self;
    this.searchCache = searchCache;
  }

  public Optional<Anime> importFromApi(String title) {
    log.info("Импорт по названию: {}", title);
    AnilistMedia foundMedia = fetchAnilistByTitle(title);
    if (foundMedia == null) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(processFranchise(foundMedia));
    } catch (Exception e) {
      log.error("Ошибка импорта '{}': {}", title, e.getMessage(), e);
      return Optional.empty();
    }
  }

  public void refreshPopularAnime(int limit) {
    log.info("Старт импорта топ-{} аниме", limit);
    String query = String.format(
        "{ Page(page: 1, perPage: %d) { media(sort: POPULARITY_DESC, type: ANIME) { %s } } }",
        limit, MEDIA_FIELDS);
    try {
      JsonNode mediaList = executeAnilistQuery(query)
          .path("data").path("Page").path("media");

      if (!mediaList.isArray()) {
        log.warn("Page query вернул не массив");
        return;
      }

      for (JsonNode node : mediaList) {
        processMediaNode(node);
      }
    } catch (Exception e) {
      log.error("Ошибка refreshPopularAnime: {}", e.getMessage(), e);
    }
    log.info("Импорт завершён");
  }

  private void processMediaNode(JsonNode node) {
    try {
      AnilistMedia media = objectMapper.treeToValue(node, AnilistMedia.class);
      processFranchise(media);
      Thread.sleep(3000);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      log.error("Прервано во время ожидания: {}", ie.getMessage(), ie);
      throw new IllegalStateException("Interrupted while processing media node", ie);
    } catch (Exception e) {
      log.error("Ошибка при обработке: {}", e.getMessage(), e);
    }
  }

  private Anime processFranchise(AnilistMedia startNode) {
    if (startNode == null) {
      return null;
    }
    log.info("Обработка: {} (anilist={}, mal={})",
        startNode.getTitle().getRomaji(), startNode.getId(), startNode.getIdMal());

    List<AnilistMedia> allMedia = collectFullChain(startNode);

    if (allMedia.isEmpty()) {
      log.warn("Не найдено подходящего медиа для: {}", startNode.getTitle().getRomaji());
      return null;
    }

    allMedia.sort(Comparator.comparing(
        this::getStartDate,
        Comparator.nullsLast(Comparator.naturalOrder())));

    boolean isOngoing = allMedia.stream().anyMatch(this::isReleasingOrUpcoming);

    long tvCount = allMedia.stream()
        .filter(this::isTvFormat)
        .filter(m -> !"NOT_YET_RELEASED".equalsIgnoreCase(m.getStatus()))
        .count();

    log.info("  Медиа: {} | TV-сезонов: {} | Онгоинг: {}",
        allMedia.size(), tvCount, isOngoing);
    allMedia.forEach(m -> log.info("    [{}] {} | {} | {} эп | mal={}",
        m.getFormat(), m.getTitle().getRomaji(),
        m.getStatus(), m.getEpisodes(), m.getIdMal()));

    return self.saveFranchise(allMedia, (int) tvCount, isOngoing);
  }

  private List<AnilistMedia> collectFullChain(AnilistMedia start) {
    Set<Long> visited    = new HashSet<>();
    Deque<Long> toFetch  = new ArrayDeque<>();
    Map<Long, AnilistMedia> result = new LinkedHashMap<>();

    registerNode(start, visited, toFetch, result);

    int safety = 0;
    while (!toFetch.isEmpty() && safety < 60) {
      Long id = toFetch.poll();
      safety++;

      AnilistMedia full = fetchAnilistByIdWithRetry(id);
      if (full != null) {
        registerNode(full, visited, toFetch, result);
      } else {
        log.warn("  Не удалось загрузить id={} (пропускаем)", id);
      }
    }

    return new ArrayList<>(result.values());
  }

  private void registerNode(AnilistMedia media, Set<Long> visited,
                            Deque<Long> toFetch, Map<Long, AnilistMedia> result) {
    if (media == null || visited.contains(media.getId())) {
      return;
    }
    visited.add(media.getId());

    if (isAcceptableFormat(media) && hasEpisodes(media)) {
      result.put(media.getId(), media);
    }

    if (media.getRelations() == null || media.getRelations().getEdges() == null) {
      return;
    }

    for (AnilistRelationEdge edge : media.getRelations().getEdges()) {
      if (edge == null || edge.getNode() == null || !isSequelOrPrequel(edge.getRelationType())) {
        continue;
      }
      Long relId = edge.getNode().getId();
      if (!visited.contains(relId) && !toFetch.contains(relId)) {
        toFetch.add(relId);
      }
    }
  }

  @Transactional
  protected Anime saveFranchise(List<AnilistMedia> allMedia, int tvCount, boolean isOngoing) {
    AnilistMedia root = allMedia.stream()
        .filter(this::isTvFormat)
        .findFirst()
        .orElse(allMedia.get(0));

    Anime anime = animeRepository.findByExternalId(root.getId())
        .orElse(new Anime());

    Map<String, Genre> genreCache = genreRepository.findAll().stream()
        .collect(Collectors.toMap(Genre::getName, g -> g, (a, b) -> a));

    fillAnimeInfo(anime, root, tvCount, genreCache, isOngoing);
    anime = animeRepository.save(anime);

    for (AnilistMedia media : allMedia) {
      saveSeasonAndEpisodes(anime, media);
    }

    searchCache.invalidateAll();
    log.info("ok '{}' | TV: {} | Онгоинг: {}", anime.getTitle(), tvCount, isOngoing);
    return anime;
  }

  private void saveSeasonAndEpisodes(Anime anime, AnilistMedia media) {
    Season season = seasonRepository.findByExternalId(media.getId())
        .orElseGet(() -> {
          Season s = new Season();
          s.setExternalId(media.getId());
          return s;
        });

    int totalEps = resolveEpisodeCount(media);

    season.setAnime(anime);
    season.setTotalEpisodes(totalEps);
    season.setIsReleased("FINISHED".equalsIgnoreCase(media.getStatus()));
    season.setReleaseDate(getStartDate(media));
    season.setFormat(media.getFormat());
    season = seasonRepository.save(season);

    episodeRepository.deleteAllBySeasonId(season.getId());
    episodeRepository.flush();

    if (totalEps == 0) {
      log.debug("  Пропуск эпизодов для {} — totalEps=0", media.getTitle().getRomaji());
      return;
    }

    Map<Integer, String> jikanTitles = fetchJikanEpisodeTitles(media.getIdMal(), totalEps);

    Map<Integer, LocalDate> airDates = buildAirDateMap(media);

    List<Episode> episodes = new ArrayList<>();
    for (int i = 1; i <= totalEps; i++) {
      Episode ep = new Episode();
      ep.setSeason(season);
      ep.setNumber(i);
      ep.setTitle(jikanTitles.getOrDefault(i, "Episode " + i));
      ep.setReleaseDate(airDates.get(i));
      episodes.add(ep);
    }

    episodeRepository.saveAll(episodes);
    log.debug("  Сохранено {} эпизодов для '{}' (jikan titles: {})",
        totalEps, media.getTitle().getRomaji(), jikanTitles.size());
  }

  private Map<Integer, String> fetchJikanEpisodeTitles(Integer malId, int totalEps) {
    if (malId == null) {
      log.debug("  idMal отсутствует — пропускаем Jikan");
      return Collections.emptyMap();
    }

    Map<Integer, String> titles = new HashMap<>();
    int page = 1;
    int pagesNeeded = (int) Math.ceil(totalEps / 100.0);
    int maxPages = Math.max(1, pagesNeeded);

    boolean hasMore = true;
    while (page <= maxPages && hasMore) {
      try {
        hasMore = fetchJikanPage(malId, page, titles);
        page++;
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        log.error("Interrupted while fetching Jikan episodes", ie);
        hasMore = false;
      } catch (Exception e) {
        log.warn("Ошибка Jikan для malId={} page={}: {}", malId, page, e.getMessage());
        hasMore = false;
      }
    }

    log.debug("  Jikan: загружено {} названий для malId={}", titles.size(), malId);
    return titles;
  }

  private boolean fetchJikanPage(
      Integer malId, int page, Map<Integer, String> titles) throws Exception {
    Thread.sleep(400);
    String url = String.format("%s/anime/%d/episodes?page=%d", JIKAN_API_URL, malId, page);
    ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
      log.warn("  Jikan вернул {} для malId={}", response.getStatusCode(), malId);
      return false;
    }

    JsonNode root = objectMapper.readTree(response.getBody());
    JsonNode data = root.path("data");

    if (!data.isArray() || data.isEmpty()) {
      return false;
    }

    for (JsonNode ep : data) {
      int epNum = ep.path("mal_id").asInt(0);
      String title = ep.path("title").asText(null);
      String titleEn = ep.path("title_romanji").asText(null);
      if (epNum > 0 && title != null && !title.isBlank()) {
        titles.put(epNum, title);
      } else if (epNum > 0 && titleEn != null && !titleEn.isBlank()) {
        titles.put(epNum, titleEn);
      }
    }

    return root.path("pagination").path("has_next_page").asBoolean(false);
  }

  private AnilistMedia fetchAnilistByIdWithRetry(Long id) {
    String query = String.format("{ Media(id: %d, type: ANIME) { %s } }", id, MEDIA_FIELDS);
    for (int attempt = 1; attempt <= 3; attempt++) {
      try {
        JsonNode node = executeAnilistQuery(query).path("data").path("Media");
        if (node.isMissingNode() || node.isNull()) {
          return null;
        }
        return objectMapper.treeToValue(node, AnilistMedia.class);
      } catch (Exception e) {
        log.warn("  fetchAnilistById({}) попытка {}/3: {}", id, attempt, e.getMessage());
        if (attempt < 3) {
          try {
            Thread.sleep(2000L * attempt);
          } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
          }
        }
      }
    }
    return null;
  }

  private AnilistMedia fetchAnilistByTitle(String title) {
    String escaped = title.replace("\"", "\\\"");
    String query = String.format(
        "{ Media(search: \"%s\", type: ANIME) { %s } }", escaped, MEDIA_FIELDS);
    try {
      JsonNode node = executeAnilistQuery(query).path("data").path("Media");
      return (node.isMissingNode() || node.isNull()) ? null
          : objectMapper.treeToValue(node, AnilistMedia.class);
    } catch (Exception e) {
      log.error("fetchAnilistByTitle({}): {}", title, e.getMessage());
      return null;
    }
  }

  private JsonNode executeAnilistQuery(String query) throws Exception {
    Thread.sleep(1200);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<Map<String, Object>> entity =
        new HttpEntity<>(Map.of("query", query), headers);
    ResponseEntity<String> response = restTemplate.exchange(
        ANILIST_API_URL, HttpMethod.POST, entity, String.class);

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new IllegalStateException("Anilist HTTP " + response.getStatusCode());
    }

    JsonNode root = objectMapper.readTree(response.getBody());
    if (root.has("errors")) {
      log.warn("Anilist errors: {}", root.path("errors").toString());
    }
    return root;
  }

  private void fillAnimeInfo(Anime anime, AnilistMedia root, long tvCount,
                             Map<String, Genre> genreCache, boolean isOngoing) {
    String title = (root.getTitle().getEnglish() != null
        && !root.getTitle().getEnglish().isBlank())
        ? root.getTitle().getEnglish()
        : root.getTitle().getRomaji();

    anime.setTitle(title);
    anime.setExternalId(root.getId());
    anime.setNumOfReleasedSeasons((int) tvCount);
    anime.setPopularityRank(root.getPopularity());
    anime.setLastUpdated(LocalDateTime.now());
    anime.setIsOngoing(isOngoing);
    anime.setDuration(root.getDuration());

    if (root.getStudios() != null && root.getStudios().getEdges() != null
        && !root.getStudios().getEdges().isEmpty()) {
      AnilistStudioEdge edge = root.getStudios().getEdges().get(0);
      if (edge != null && edge.getNode() != null) {
        anime.setStudio(edge.getNode().getName());
      }
    }

    Set<Genre> genres = new HashSet<>();
    if (root.getGenres() != null) {
      for (String genreName : root.getGenres()) {
        if (genreName == null || genreName.isBlank()) {
          continue;
        }
        genres.add(genreCache.computeIfAbsent(
            genreName.trim(),
            n -> genreRepository.save(new Genre(null, n, null))));
      }
    }
    anime.setGenres(genres);
  }

  private int resolveEpisodeCount(AnilistMedia media) {
    if (media.getEpisodes() != null && media.getEpisodes() > 0) {
      return media.getEpisodes();
    }
    if (isReleasingOrUpcoming(media)) {
      int fromSchedule = maxEpisodeFromSchedule(media);
      if (fromSchedule > 0) {
        return fromSchedule;
      }
    }
    return 0;
  }

  private boolean isAcceptableFormat(AnilistMedia m) {
    if (m == null || m.getFormat() == null) {
      return false;
    }
    return switch (m.getFormat().toUpperCase()) {
      case "TV", "TV_SHORT", "OVA", "ONA", "MOVIE", "SPECIAL" -> true;
      default -> false;
    };
  }

  private boolean isTvFormat(AnilistMedia m) {
    if (m == null || m.getFormat() == null) {
      return false;
    }
    String f = m.getFormat().toUpperCase();
    return "TV".equals(f) || "TV_SHORT".equals(f);
  }

  private boolean isReleasingOrUpcoming(AnilistMedia m) {
    if (m == null || m.getStatus() == null) {
      return false;
    }
    String s = m.getStatus().toUpperCase();
    return "RELEASING".equals(s) || "NOT_YET_RELEASED".equals(s);
  }

  private boolean hasEpisodes(AnilistMedia m) {
    return m.getEpisodes() == null || m.getEpisodes() > 0;
  }

  private boolean isSequelOrPrequel(String relationType) {
    if (relationType == null) {
      return false;
    }
    String r = relationType.toUpperCase();
    return "SEQUEL".equals(r) || "PREQUEL".equals(r);
  }

  private int maxEpisodeFromSchedule(AnilistMedia m) {
    if (m.getAiringSchedule() == null || m.getAiringSchedule().getNodes() == null) {
      return 0;
    }
    return m.getAiringSchedule().getNodes().stream()
        .filter(n -> n.getEpisode() != null)
        .mapToInt(AnilistAiringScheduleNode::getEpisode)
        .max().orElse(0);
  }

  private Map<Integer, LocalDate> buildAirDateMap(AnilistMedia m) {
    if (m.getAiringSchedule() == null || m.getAiringSchedule().getNodes() == null) {
      return Collections.emptyMap();
    }
    Map<Integer, LocalDate> map = new HashMap<>();
    for (AnilistAiringScheduleNode node : m.getAiringSchedule().getNodes()) {
      if (node.getEpisode() != null && node.getAiringAt() != null) {
        LocalDate date = Instant.ofEpochSecond(node.getAiringAt())
            .atOffset(ZoneOffset.UTC).toLocalDate();
        map.put(node.getEpisode(), date);
      }
    }
    return map;
  }

  private LocalDate getStartDate(AnilistMedia m) {
    if (m == null || m.getStartDate() == null || m.getStartDate().getYear() == null) {
      return null;
    }
    return LocalDate.of(
        m.getStartDate().getYear(),
        m.getStartDate().getMonth() != null ? m.getStartDate().getMonth() : 1,
        m.getStartDate().getDay() != null ? m.getStartDate().getDay() : 1);
  }
}