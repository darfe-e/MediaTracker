package org.example.animetracker.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.example.animetracker.controller.api.AnimeControllerApi;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.mapper.AnimeMapper;
import org.example.animetracker.service.AnimeImportService;
import org.example.animetracker.service.AnimeService;
import org.example.animetracker.service.AsyncAnimeImportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/anime-catalogue")
public class AnimeController implements AnimeControllerApi {

  private final AnimeService animeService;
  private final AnimeImportService animeImportService;
  private final AsyncAnimeImportService asyncImportService;

  @GetMapping("/{id}")
  public ResponseEntity<AnimeDetailedDto> getById(@PathVariable Long id) {
    AnimeDetailedDto dto = animeService.findByIdWithoutProblem(id);
    return ResponseEntity.ok(dto);
  }

  @GetMapping
  public ResponseEntity<List<AnimeDto>> getByStudioAndTitle(
      @RequestParam(required = false) String studio,
      @RequestParam(required = false) String title) {
    List<AnimeDto> result = animeService.findByStudioAndName(studio, title);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/")
  public ResponseEntity<Page<AnimeDto>> getAllSorted(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<AnimeDto> result = animeService.getAllSortedByPopularity(pageable);
    return ResponseEntity.ok(result);
  }

  /**
   * Поиск аниме с быстрым ответом.
   *
   * Проблема со старым кодом: importFromApi всегда делал полный BFS + Jikan
   * даже если аниме уже было в БД — клиент ждал 30-60 секунд.
   *
   * Новая стратегия (stale-while-revalidate):
   *
   * 1. Ищем в нашей БД по названию (мгновенно)
   * 2. Если НАЙДЕНО:
   *    - Возвращаем данные из БД немедленно (~10мс)
   *    - Запускаем фоновое обновление через AsyncAnimeImportService
   *    - В заголовке X-Background-Update-TaskId передаём id фоновой задачи
   *      (клиент может следить за обновлением через GET /import/tasks/{id})
   * 3. Если НЕ НАЙДЕНО в БД:
   *    - Делаем синхронный импорт (полный BFS — первый раз неизбежно медленно)
   *    - Возвращаем результат после сохранения
   *
   * Пример: первый поиск "One Piece" — ждём ~5 мин.
   *         второй поиск "One Piece" — ответ < 100мс, данные обновляются в фоне.
   */
  @GetMapping("/search")
  public ResponseEntity<AnimeDto> searchAnime(@RequestParam String title) {
    // 1. Ищем в локальной БД
    return animeService.findByTitle(title)
        .map(animeFromDb -> {
          // Аниме уже есть — отвечаем мгновенно, обновляем в фоне
          String taskId = asyncImportService.startSingleImport(title);

          return ResponseEntity.ok()
              .header("X-Data-Source", "database")
              .header("X-Background-Update-TaskId", taskId)
              .body(AnimeMapper.animeToDto(animeFromDb));
        })
        .orElseGet(() -> animeImportService.importFromApi(title)
              .map(anime -> ResponseEntity.ok()
                  .header("X-Data-Source", "anilist-api")
                  .body(AnimeMapper.animeToDto(anime)))
              .orElse(ResponseEntity.notFound().build())
        );
  }

  @GetMapping("/search-jpql")
  public ResponseEntity<Page<AnimeDto>> searchJpql(
      @RequestParam String genre,
      @RequestParam int minSeasons,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("popularityRank")));
    Page<AnimeDto> result = animeService.findByGenreAndMinSeasons(genre, minSeasons, pageable);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/search-native")
  public ResponseEntity<Page<AnimeDto>> searchNative(
      @RequestParam String genre,
      @RequestParam int minSeasons,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("popularity_rank")));
    Page<AnimeDto> result = animeService.findByGenreAndMinSeasonsNative(
        genre, minSeasons, pageable);
    return ResponseEntity.ok(result);
  }
}