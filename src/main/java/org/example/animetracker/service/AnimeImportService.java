package org.example.animetracker.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.cache.AnimeSearchCache;
import org.example.animetracker.dto.external.AnilistAiringScheduleNode;
import org.example.animetracker.dto.external.AnilistCoverImage;
import org.example.animetracker.dto.external.AnilistMedia;
import org.example.animetracker.dto.external.AnilistRelationEdge;
import org.example.animetracker.dto.external.AnilistStudioEdge;
import org.example.animetracker.dto.external.AnilistStudios;
import org.example.animetracker.exception.AnimeImportException;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Episode;
import org.example.animetracker.model.Season;
import org.example.animetracker.model.Genre;
import org.example.animetracker.model.SystemTask;
import org.example.animetracker.model.ImportTask;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.EpisodeRepository;
import org.example.animetracker.repository.GenreRepository;
import org.example.animetracker.repository.SeasonRepository;
import org.example.animetracker.repository.SystemTaskRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class AnimeImportService {
  private final SystemTaskRepository taskRepository;
  private final AnimeRepository animeRepository;
  private final GenreRepository genreRepository;
  private final SeasonRepository seasonRepository;
  private final EpisodeRepository episodeRepository;
  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;
  private final AnimeSearchCache searchCache;
  private final AnimeImportService self;

  private final ConcurrentHashMap<String, ReentrantLock> franchiseLocks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, ReentrantLock> seasonLocks = new ConcurrentHashMap<>();

  private static final int MIN_EPISODES_FOR_SEASON = 6;
  private static final String ANILIST_API_URL = "https://graphql.anilist.co";
  private static final String JIKAN_API_URL = "https://api.jikan.moe/v4";

  // Вынесенные константы
  private static final String NOT_YET_RELEASED = "NOT_YET_RELEASED";
  private static final String RELEASING_STATUS = "RELEASING";
  private static final String FINISHED_STATUS = "FINISHED";
  private static final String MEDIA_KEY = "media";

  private static final String MEDIA_FIELDS = """
      id idMal popularity
      title { romaji english }
      format status episodes duration
      startDate { year month day }
      studios(isMain: true) { edges { node { name } } }
      genres
      coverImage { extraLarge }
      airingSchedule(notYetAired: true) { nodes { airingAt episode } }
      relations { edges { relationType node { id } } }
      """;

  public AnimeImportService(AnimeRepository animeRepository,
                            GenreRepository genreRepository,
                            SeasonRepository seasonRepository,
                            EpisodeRepository episodeRepository,
                            RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            @Lazy AnimeImportService self,
                            AnimeSearchCache searchCache,
                            SystemTaskRepository taskRepository) {
    this.animeRepository = animeRepository;
    this.genreRepository = genreRepository;
    this.seasonRepository = seasonRepository;
    this.episodeRepository = episodeRepository;
    this.restTemplate = restTemplate;
    this.objectMapper = objectMapper;
    this.self = self;
    this.searchCache = searchCache;
    this.taskRepository = taskRepository;
  }

  private ReentrantLock getFranchiseLock(AnilistMedia root) {
    String key = animeRepository.findByExternalId(root.getId())
        .map(a -> "db:" + a.getId())
        .orElse("title:" + resolveTitle(root).toLowerCase().replaceAll("\\s+", "_"));
    return franchiseLocks.computeIfAbsent(key, k -> new ReentrantLock(true));
  }

  private ReentrantLock getSeasonLock(Long externalSeasonId) {
    return seasonLocks.computeIfAbsent(externalSeasonId, k -> new ReentrantLock(true));
  }

  public Optional<Anime> importFromApi(String title) {
    log.info("Импорт по названию: {}", title);
    AnilistMedia found = fetchAnilistByTitle(title);
    if (found == null) {
      return Optional.empty();
    }
    try {
      return Optional.ofNullable(processFranchise(found));
    } catch (Exception e) {
      log.error("Ошибка импорта '{}': {}", title, e.getMessage(), e);
      return Optional.empty();
    }
  }

  public void refreshPopularAnime(int limit) {
    log.info("Старт импорта топ-{} аниме", limit);
    String query = String.format(
        "{ Page(page:1,perPage:%d){ media(sort:POPULARITY_DESC,type:ANIME){ %s } } }",
        limit, MEDIA_FIELDS);
    try {
      JsonNode list = executeAnilistQuery(query).path("data").path("Page").path(MEDIA_KEY);
      if (!list.isArray()) {
        log.warn("Не массив");
        return;
      }
      for (JsonNode node : list) {
        processMediaNode(node);
      }
    } catch (Exception e) {
      log.error("refreshPopularAnime: {}", e.getMessage(), e);
    }
    log.info("Импорт завершён");
  }

  Anime processFranchise(AnilistMedia startNode) {
    if (startNode == null) {
      return null;
    }
    ReentrantLock lock = getFranchiseLock(startNode);
    lock.lock();
    try {
      return processFranchiseLocked(startNode);
    } finally {
      lock.unlock();
    }
  }

  private Anime processFranchiseLocked(AnilistMedia startNode) {
    log.info("Обработка: {} (id={})", startNode.getTitle().getRomaji(), startNode.getId());
    List<AnilistMedia> chain = collectFullChain(startNode);

    if (chain.isEmpty()) {
      log.warn("Пустая цепочка для {}", startNode.getTitle().getRomaji());
      return null;
    }

    chain.sort(Comparator.comparing(this::getStartDate,
        Comparator.nullsLast(Comparator.naturalOrder())));

    LocalDate today = LocalDate.now();
    LocalDate twoWeeksLater = today.plusWeeks(2);

    boolean isOngoing = determineIfOngoing(chain, today, twoWeeksLater);
    boolean isAnnounced = determineIfAnnounced(chain, isOngoing, today, twoWeeksLater);
    long tvCount = calculateTvCount(chain);

    log.info("  chain:{} | seasons:{} | ongoing:{} | announced:{}",
        chain.size(), tvCount, isOngoing, isAnnounced);

    return self.saveFranchise(chain, (int) tvCount, isOngoing, isAnnounced);
  }

  private boolean determineIfOngoing(List<AnilistMedia> chain, LocalDate today,
                                     LocalDate twoWeeksLater) {
    return chain.stream().anyMatch(m -> {
      if (!RELEASING_STATUS.equalsIgnoreCase(m.getStatus())) {
        return false;
      }
      LocalDate sd = getStartDate(m);
      if (sd != null && !sd.isAfter(today)) {
        return hasEpisodeWithin(m, twoWeeksLater);
      }
      return sd != null && !sd.isAfter(twoWeeksLater);
    });
  }

  private boolean determineIfAnnounced(List<AnilistMedia> chain, boolean isOngoing,
                                       LocalDate today, LocalDate twoWeeksLater) {
    if (isOngoing) {
      return false;
    }
    return chain.stream().anyMatch(m -> {
      String status = m.getStatus();
      if (NOT_YET_RELEASED.equalsIgnoreCase(status)) {
        return true;
      }
      if (RELEASING_STATUS.equalsIgnoreCase(status)) {
        LocalDate sd = getStartDate(m);
        return sd == null || sd.isAfter(twoWeeksLater);
      }
      if (FINISHED_STATUS.equalsIgnoreCase(status)) {
        LocalDate sd = getStartDate(m);
        return sd != null && sd.isAfter(today);
      }
      return false;
    });
  }

  private long calculateTvCount(List<AnilistMedia> chain) {
    return chain.stream()
        .filter(this::isSerialFormat)
        .filter(m -> !NOT_YET_RELEASED.equalsIgnoreCase(m.getStatus()))
        .filter(m -> resolveEpisodeCount(m) >= MIN_EPISODES_FOR_SEASON)
        .count();
  }

  List<AnilistMedia> collectFullChain(AnilistMedia start) {
    Set<Long> visited = new HashSet<>();
    Deque<Long> toFetch = new ArrayDeque<>();
    Map<Long, AnilistMedia> result = new LinkedHashMap<>();
    registerNode(start, visited, toFetch, result);
    int safety = 0;
    while (!toFetch.isEmpty() && safety < 60) {
      Long id = toFetch.poll();
      safety++;
      AnilistMedia m = fetchAnilistByIdWithRetry(id);
      if (m != null) {
        registerNode(m, visited, toFetch, result);
      } else {
        log.warn("  Не загружен id={}", id);
      }
    }
    return new ArrayList<>(result.values());
  }

  private void registerNode(AnilistMedia m, Set<Long> visited,
                            Deque<Long> toFetch, Map<Long, AnilistMedia> result) {
    if (m == null || visited.contains(m.getId())) {
      return;
    }
    visited.add(m.getId());
    if (isAcceptableFormat(m) && hasEpisodesOrPending(m)) {
      result.put(m.getId(), m);
    }
    if (m.getRelations() == null || m.getRelations().getEdges() == null) {
      return;
    }
    for (AnilistRelationEdge e : m.getRelations().getEdges()) {
      if (e == null || e.getNode() == null || !isSequelOrPrequel(e.getRelationType())) {
        continue;
      }
      Long rid = e.getNode().getId();
      if (!visited.contains(rid) && !toFetch.contains(rid)) {
        toFetch.add(rid);
      }
    }
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  protected Anime saveFranchise(List<AnilistMedia> chain, int tvCount,
                                boolean isOngoing, boolean isAnnounced) {
    AnilistMedia root = chain.stream()
        .filter(this::isSerialFormat)
        .findFirst()
        .orElse(chain.get(0));

    Anime anime = animeRepository.findByExternalId(root.getId())
        .orElseGet(() -> chain.stream()
            .map(m -> seasonRepository.findByExternalId(m.getId()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Season::getAnime)
            .findFirst()
            .orElse(new Anime()));

    Map<String, Genre> gc = genreRepository.findAll().stream()
        .collect(Collectors.toMap(Genre::getName, g -> g, (a, b) -> a));

    fillAnimeInfo(anime, root, tvCount, gc, isOngoing, isAnnounced);
    anime = animeRepository.save(anime);

    for (AnilistMedia m : chain) {
      saveSeasonAndEpisodes(anime, m);
    }

    cleanupOrphanAnime(chain, anime.getId());
    searchCache.invalidateAll();

    log.info("Сохранено '{}' | seasons:{} | ongoing:{} | announced:{}",
        anime.getTitle(), tvCount, isOngoing, isAnnounced);

    return anime;
  }

  private void saveSeasonAndEpisodes(Anime anime, AnilistMedia media) {
    ReentrantLock seasonLock = getSeasonLock(media.getId());
    seasonLock.lock();
    try {
      doSaveSeasonAndEpisodes(anime, media);
    } finally {
      seasonLock.unlock();
      seasonLocks.remove(media.getId(), seasonLock);
    }
  }

  private void doSaveSeasonAndEpisodes(Anime anime, AnilistMedia media) {
    Season season = seasonRepository.findByExternalId(media.getId())
        .orElseGet(() -> {
          Season s = new Season();
          s.setExternalId(media.getId());
          return s;
        });

    int totalEps = resolveEpisodeCount(media);
    season.setAnime(anime);
    season.setTotalEpisodes(totalEps);
    season.setIsReleased(FINISHED_STATUS.equalsIgnoreCase(media.getStatus()));
    season.setReleaseDate(getStartDate(media));
    season.setFormat(media.getFormat());
    season = seasonRepository.save(season);

    final Long seasonId = season.getId();
    episodeRepository.deleteAllBySeasonId(seasonId);
    episodeRepository.flush();

    if (totalEps == 0) {
      log.debug("  Нет эпизодов для {}", media.getTitle().getRomaji());
      return;
    }

    Map<Integer, String> titles = fetchJikanEpisodeTitles(media.getIdMal(), totalEps);
    Map<Integer, LocalDate> airDates = buildAirDateMap(media);
    List<Episode> eps = new ArrayList<>();

    for (int i = 1; i <= totalEps; i++) {
      Episode ep = new Episode();
      ep.setSeason(season);
      ep.setNumber(i);
      ep.setTitle(titles.getOrDefault(i, "Episode " + i));
      ep.setReleaseDate(airDates.get(i));
      eps.add(ep);
    }

    episodeRepository.saveAll(eps);
    log.debug("  {} эп → '{}'", totalEps, media.getTitle().getRomaji());
  }

  @Scheduled(cron = "0 0 */2 * * *")
  @Transactional
  public void removeDuplicateEpisodes() {
    log.info("=== Очистка дублей эпизодов ===");
    try {
      List<Long> seasonIds = episodeRepository.findSeasonIdsWithDuplicates();
      if (seasonIds.isEmpty()) {
        log.info("Дублей нет");
        return;
      }
      log.info("  Сезонов с дублями: {}", seasonIds.size());
      for (Long sid : seasonIds) {
        List<Long> toDelete = episodeRepository.findDuplicateEpisodeIds(sid);
        if (!toDelete.isEmpty()) {
          episodeRepository.deleteAllById(toDelete);
          log.info("  Удалено {} дублей в сезоне {}", toDelete.size(), sid);
        }
      }
      log.info("=== Очистка дублей завершена ===");
    } catch (Exception e) {
      log.error("Ошибка очистки дублей: {}", e.getMessage(), e);
    }
  }

  private void cleanupOrphanAnime(List<AnilistMedia> chain, Long keepId) {
    for (AnilistMedia m : chain) {
      animeRepository.findByExternalId(m.getId()).ifPresent(orphan -> {
        if (!orphan.getId().equals(keepId)) {
          log.info("  Удаляем дубль id={} '{}'", orphan.getId(), orphan.getTitle());
          try {
            animeRepository.deleteById(orphan.getId());
          } catch (Exception e) {
            log.warn("  Не удалось {}: {}", orphan.getId(), e.getMessage());
          }
        }
      });
    }
  }

  private boolean hasEpisodeWithin(AnilistMedia m, LocalDate deadline) {
    if (m.getAiringSchedule() == null || m.getAiringSchedule().getNodes() == null) {
      return false;
    }
    return m.getAiringSchedule().getNodes().stream()
        .filter(n -> n.getAiringAt() != null)
        .anyMatch(n -> !Instant.ofEpochSecond(n.getAiringAt())
            .atOffset(ZoneOffset.UTC).toLocalDate().isAfter(deadline));
  }

  private Map<Integer, String> fetchJikanEpisodeTitles(Integer malId, int total) {
    if (malId == null) {
      return Collections.emptyMap();
    }
    Map<Integer, String> titles = new HashMap<>();
    int page = 1;
    int max = Math.max(1, (int) Math.ceil(total / 100.0));
    boolean has = true;
    boolean interrupted = false;
    boolean errorOccurred = false;

    while (page <= max && has && !interrupted && !errorOccurred) {
      try {
        has = fetchJikanPage(malId, page, titles);
        page++;
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        interrupted = true;
      } catch (Exception e) {
        log.warn("Jikan malId={} p={}: {}", malId, page - 1, e.getMessage());
        errorOccurred = true;
      }
    }
    return titles;
  }

  private boolean fetchJikanPage(Integer malId, int page, Map<Integer, String> titles)
      throws InterruptedException, JsonProcessingException {
    Thread.sleep(400);
    String url = String.format("%s/anime/%d/episodes?page=%d", JIKAN_API_URL, malId, page);
    ResponseEntity<String> r = restTemplate.getForEntity(url, String.class);

    if (!r.getStatusCode().is2xxSuccessful() || r.getBody() == null) {
      return false;
    }

    JsonNode root = objectMapper.readTree(r.getBody());
    JsonNode data = root.path("data");

    if (!data.isArray() || data.isEmpty()) {
      return false;
    }

    for (JsonNode ep : data) {
      int n = ep.path("mal_id").asInt(0);
      String t = ep.path("title").asText(null);
      if (n > 0 && t != null && !t.isBlank()) {
        titles.put(n, t);
      }
    }
    return root.path("pagination").path("has_next_page").asBoolean(false);
  }

  protected AnilistMedia fetchAnilistByIdWithRetry(Long id) {
    String query = String.format("{ Media(id:%d,type:ANIME){ %s } }", id, MEDIA_FIELDS);
    for (int attempt = 1; attempt <= 3; attempt++) {
      try {
        JsonNode node = executeAnilistQuery(query).path("data").path("Media");
        if (node.isMissingNode() || node.isNull()) {
          return null;
        }
        return objectMapper.treeToValue(node, AnilistMedia.class);
      } catch (Exception e) {
        log.warn("  fetchById({}) {}/3: {}", id, attempt, e.getMessage());
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
    String query = String.format("{ Media(search:\"%s\",type:ANIME){ %s } }",
        title.replace("\"", "\\\""), MEDIA_FIELDS);
    try {
      JsonNode node = executeAnilistQuery(query).path("data").path("Media");
      if (node.isMissingNode() || node.isNull()) {
        return null;
      }
      return objectMapper.treeToValue(node, AnilistMedia.class);
    } catch (Exception e) {
      log.error("fetchByTitle({}): {}", title, e.getMessage());
      return null;
    }
  }

  private JsonNode executeAnilistQuery(String query) throws Exception {
    Thread.sleep(1200);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    HttpEntity<Map<String, String>> entity = new HttpEntity<>(Map.of("query", query), headers);
    ResponseEntity<String> r = restTemplate.exchange(ANILIST_API_URL,
        HttpMethod.POST, entity, String.class);

    if (!r.getStatusCode().is2xxSuccessful()) {
      throw new IllegalStateException("Anilist " + r.getStatusCode());
    }

    JsonNode root = objectMapper.readTree(r.getBody());
    if (root.has("errors")) {
      log.warn("Anilist errors: {}", root.path("errors"));
    }
    return root;
  }

  private void fillAnimeInfo(Anime anime, AnilistMedia root, long tvCount,
                             Map<String, Genre> gc, boolean isOngoing, boolean isAnnounced) {
    anime.setTitle(resolveTitle(root));
    anime.setExternalId(root.getId());
    anime.setNumOfReleasedSeasons((int) tvCount);
    anime.setPopularityRank(root.getPopularity());
    anime.setLastUpdated(LocalDateTime.now());
    anime.setIsOngoing(isOngoing);
    anime.setIsAnnounced(isAnnounced);
    anime.setDuration(root.getDuration());
    anime.setPosterUrl(resolvePoster(root.getCoverImage()));
    anime.setStudio(resolveStudioName(root.getStudios()));
    anime.setGenres(resolveGenres(root.getGenres(), gc));
  }

  private String resolveTitle(AnilistMedia r) {
    String en = r.getTitle().getEnglish();
    if (en != null && !en.isBlank()) {
      return en;
    }
    return r.getTitle().getRomaji();
  }

  private String resolvePoster(AnilistCoverImage image) {
    if (image == null) {
      return null;
    }
    if (image.getExtraLarge() != null) {
      return image.getExtraLarge();
    }
    if (image.getLarge() != null) {
      return image.getLarge();
    }
    return image.getMedium();
  }

  private String resolveStudioName(AnilistStudios studios) {
    if (studios == null || studios.getEdges() == null || studios.getEdges().isEmpty()) {
      return null;
    }
    AnilistStudioEdge edge = studios.getEdges().get(0);
    if (edge != null && edge.getNode() != null) {
      return edge.getNode().getName();
    }
    return null;
  }

  private Set<Genre> resolveGenres(List<String> names, Map<String, Genre> cache) {
    if (names == null) {
      return new HashSet<>();
    }
    return names.stream()
        .filter(n -> n != null && !n.isBlank())
        .map(String::trim)
        .map(n -> cache.computeIfAbsent(n, k -> genreRepository.save(new Genre(null, k, null))))
        .collect(Collectors.toSet());
  }

  private int resolveEpisodeCount(AnilistMedia m) {
    if (m.getEpisodes() != null && m.getEpisodes() > 0) {
      return m.getEpisodes();
    }
    if (isReleasingOrUpcoming(m)) {
      int n = maxEpisodeFromSchedule(m);
      if (n > 0) {
        return n;
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

  private boolean isSerialFormat(AnilistMedia m) {
    if (m == null || m.getFormat() == null) {
      return false;
    }
    return switch (m.getFormat().toUpperCase()) {
      case "TV", "TV_SHORT", "ONA" -> true;
      default -> false;
    };
  }

  private boolean isReleasingOrUpcoming(AnilistMedia m) {
    if (m == null || m.getStatus() == null) {
      return false;
    }
    String status = m.getStatus().toUpperCase();
    return RELEASING_STATUS.equals(status) || NOT_YET_RELEASED.equals(status);
  }

  private boolean hasEpisodesOrPending(AnilistMedia m) {
    if (m.getEpisodes() != null && m.getEpisodes() > 0) {
      return true;
    }
    return isReleasingOrUpcoming(m);
  }

  private boolean isSequelOrPrequel(String relationType) {
    if (relationType == null) {
      return false;
    }
    String rt = relationType.toUpperCase();
    return "SEQUEL".equals(rt) || "PREQUEL".equals(rt);
  }

  private int maxEpisodeFromSchedule(AnilistMedia m) {
    if (m.getAiringSchedule() == null || m.getAiringSchedule().getNodes() == null) {
      return 0;
    }
    return m.getAiringSchedule().getNodes().stream()
        .filter(n -> n.getEpisode() != null)
        .mapToInt(AnilistAiringScheduleNode::getEpisode)
        .max()
        .orElse(0);
  }

  private Map<Integer, LocalDate> buildAirDateMap(AnilistMedia m) {
    if (m.getAiringSchedule() == null || m.getAiringSchedule().getNodes() == null) {
      return Collections.emptyMap();
    }
    Map<Integer, LocalDate> map = new HashMap<>();
    for (AnilistAiringScheduleNode n : m.getAiringSchedule().getNodes()) {
      if (n.getEpisode() != null && n.getAiringAt() != null) {
        map.put(n.getEpisode(), Instant.ofEpochSecond(n.getAiringAt())
            .atOffset(ZoneOffset.UTC).toLocalDate());
      }
    }
    return map;
  }

  private LocalDate getStartDate(AnilistMedia m) {
    if (m == null || m.getStartDate() == null || m.getStartDate().getYear() == null) {
      return null;
    }
    int year = m.getStartDate().getYear();
    int month = m.getStartDate().getMonth() != null ? m.getStartDate().getMonth() : 1;
    int day = m.getStartDate().getDay() != null ? m.getStartDate().getDay() : 1;
    return LocalDate.of(year, month, day);
  }

  public void refreshPopularAnimeWithProgress(int limit, ImportTask task) throws Exception {
    String query = String.format(
        "{ Page(page:1,perPage:%d){ media(sort:POPULARITY_DESC,type:ANIME){ %s } } }",
        limit, MEDIA_FIELDS);
    try {
      JsonNode list = fetchMediaList(query);
      task.setTotalCount(list.size());
      for (JsonNode node : list) {
        processSingleMedia(node, task);
      }
    } catch (IOException e) {
      throw new AnimeImportException("Fetch failed", e);
    }
  }

  private JsonNode fetchMediaList(String query) throws Exception {
    JsonNode list = executeAnilistQuery(query).path("data").path("Page").path(MEDIA_KEY);
    if (!list.isArray()) {
      throw new IOException("Not array");
    }
    return list;
  }

  private void processMediaNode(JsonNode node) {
    try {
      processFranchise(objectMapper.treeToValue(node, AnilistMedia.class));
      Thread.sleep(3000);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(ie);
    } catch (Exception e) {
      log.error("processMediaNode: {}", e.getMessage(), e);
    }
  }

  private void processSingleMedia(JsonNode node, ImportTask task) {
    try {
      processFranchise(objectMapper.treeToValue(node, AnilistMedia.class));
      task.setProcessedCount(task.getProcessedCount() + 1);
      TimeUnit.MILLISECONDS.sleep(3000);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      throw new AnimeImportException("Interrupted", ie);
    } catch (JsonProcessingException e) {
      log.error("JSON: {}", e.getMessage());
    } catch (Exception e) {
      log.error("processSingleMedia: {}", e.getMessage(), e);
    }
  }

  @Async("importExecutor")
  @Scheduled(cron = "0 0 */6 * * *")
  public void refreshOngoingAnime() {
    log.info("=== Обновление онгоингов ===");
    animeRepository.findExternalIdsByIsOngoing(true).forEach(id -> {
      try {
        AnilistMedia m = fetchAnilistByIdWithRetry(id);
        if (m != null) {
          processFranchise(m);
        }
        Thread.sleep(3000);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        log.error("refreshOngoing id={}: {}", id, e.getMessage());
      }
    });
    log.info("=== Обновление онгоингов завершено ===");
  }

  @Async("importExecutor")
  @Scheduled(cron = "0 0 */6 * * *") // каждые 6 часов
  public void refreshActiveAnime() {

    String taskName = "ANIME_UPDATE";
    LocalDateTime now = LocalDateTime.now();

    SystemTask taskInfo = taskRepository.findById(taskName)
        .orElse(new SystemTask(taskName, LocalDateTime.MIN));

    long hoursPassed = java.time.Duration.between(taskInfo.getLastRun(), now).toHours();

    if (hoursPassed < 6) {
      log.info("Обновление пропущено. Прошло времени: {} ч.", hoursPassed);
      return;
    }
    log.info("=== Обновление онгоингов и анонсов ===");

    java.util.List<Long> ongoingIds = animeRepository.findExternalIdsByIsOngoing(true);
    log.info("Онгоингов: {}", ongoingIds.size());
    ongoingIds.forEach(id -> {
      try {
        AnilistMedia m = fetchAnilistByIdWithRetry(id);
        if (m != null) {
          processFranchise(m);
        }
        Thread.sleep(1500); // быстрее чем для завершённых
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        log.error("refreshActive id={}: {}", id, e.getMessage());
      }
    });

    java.util.List<Long> announcedIds = animeRepository.findExternalIdsByIsAnnounced(true);
    log.info("Анонсов: {}", announcedIds.size());
    announcedIds.forEach(id -> {
      try {
        AnilistMedia m = fetchAnilistByIdWithRetry(id);
        if (m != null) {
          processFranchise(m);
        }
        Thread.sleep(1500);
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        log.error("refreshAnnounced id={}: {}", id, e.getMessage());
      }
    });

    log.info("=== Обновление активных завершено ({} + {} аниме) ===",
        ongoingIds.size(), announcedIds.size());
  }

  @Scheduled(fixedRate = 3600000)
  public void refreshFinishedAnime() {
    String taskName = "REFRESH_FINISHED_ANIME";

    SystemTask task = taskRepository.findById(taskName)
        .orElse(new SystemTask(taskName, LocalDateTime.now().minusDays(6), 0L));

    boolean isInProgress = task.getLastProcessedId() > 0;
    boolean isTimeTodo = task.getLastRun().isBefore(LocalDateTime.now().minusDays(5));

    if (!isTimeTodo && !isInProgress) {
      return;
    }

    self.processBatchAsync(task);
  }

  @Async("importExecutor")
  public void processBatchAsync(SystemTask task) {
    log.info("=== Обновление завершённых аниме (пакет) ===");

    List<Anime> batch = animeRepository
        .findTop30ByIsOngoingFalseAndIdGreaterThanOrderByIdAsc(task.getLastProcessedId());

    if (batch.isEmpty()) {
      log.info("=== Обновление завершённых завершено полностью ===");
      task.setLastProcessedId(0L);
      task.setLastRun(LocalDateTime.now());
      taskRepository.save(task);
      return;
    }

    log.info("Устаревших записей в текущей пачке: {}", batch.size());

    batch.forEach(anime -> {
      try {
        AnilistMedia m = fetchAnilistByIdWithRetry(anime.getExternalId());
        if (m != null) {
          processFranchise(m);                                         // Твоя логика обработки
        }
        task.setLastProcessedId(anime.getId());                          // Фиксируем прогресс в БД
        Thread.sleep(2500);                                              // Твоя задержка
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        log.error("refreshFinished id={}: {}", anime.getExternalId(), e.getMessage());
      }
    });

    taskRepository.save(task);
    log.info("=== Пакет завершён, ID остановки: {} ===", task.getLastProcessedId());
  }

  public void importPageFromAnilist(int page, int size) throws Exception {
    int anilistPage = page + 1;
    String query = String.format(
        "{ Page(page:%d,perPage:%d){ media(sort:POPULARITY_DESC,type:ANIME){ %s } } }",
        anilistPage, size, MEDIA_FIELDS);

    JsonNode list = executeAnilistQuery(query).path("data").path("Page").path(MEDIA_KEY);

    if (!list.isArray() || list.isEmpty()) {
      log.info("Страница {} пуста — подгрузка завершена", anilistPage);
      return;
    }

    log.info("Подгрузка страницы {} — {} аниме", anilistPage, list.size());
    for (JsonNode node : list) {
      try {
        AnilistMedia media = objectMapper.treeToValue(node, AnilistMedia.class);
        if (animeRepository.findByExternalId(media.getId()).isEmpty()) {
          processFranchise(media);
        }
        Thread.sleep(1200); // уважаем rate-limit AniList
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
        return;
      } catch (Exception e) {
        log.warn("  Пропуск аниме при подгрузке: {}", e.getMessage());
      }
    }
  }
}