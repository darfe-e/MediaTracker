package org.example.animetracker.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.example.animetracker.controller.api.AnimeControllerApi;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.service.AnimeImportService;
import org.example.animetracker.service.AnimeService;
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

  @GetMapping("/{id}")
  public ResponseEntity<AnimeDetailedDto> getById(@PathVariable Long id) {
    AnimeDetailedDto dto = animeService.findByIdWithoutProblem(id);
    return ResponseEntity.ok(dto);
  }

  @GetMapping("/health")
  public ResponseEntity<String> healthCheck() {
    return ResponseEntity.ok("ALIVE"); // Возвращает статус 200 OK
  }

  @GetMapping
  public ResponseEntity<List<AnimeDto>> getByStudio(
      @RequestParam(required = false) String studio) {
    List<AnimeDto> result = animeService.findByStudio(studio);
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

  @GetMapping("/search")
  public ResponseEntity<org.example.animetracker.dto.AnimeDto> searchAnime(
      @RequestParam String title) {

    return animeService.findByTitle(title)
        .map(animeFromDb -> ResponseEntity.ok()
            .header("X-Data-Source", "database")
            .body(org.example.animetracker.mapper.AnimeMapper.animeToDto(animeFromDb)))
        .orElseGet(() -> animeImportService.importFromApi(title)
            .map(anime -> ResponseEntity.ok()
                .header("X-Data-Source", "anilist-api")
                .body(org.example.animetracker.mapper.AnimeMapper.animeToDto(anime)))
            .orElse(ResponseEntity.notFound().build()));
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

  @GetMapping("/filter")
  public ResponseEntity<Page<AnimeDto>> filterAnime(
      @RequestParam(required = false) String studio,
      @RequestParam(required = false) String genre,
      @RequestParam(required = false) Integer minEpisodes,
      @RequestParam(required = false) Boolean isAiring,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("popularityRank").descending());
    Page<AnimeDto> result = animeService.findByFilters(
        studio, genre, minEpisodes, isAiring, pageable);
    return ResponseEntity.ok(result);
  }

  @GetMapping("/suggest")
  public ResponseEntity<List<AnimeDto>> suggest(@RequestParam String q) {
    return ResponseEntity.ok(animeService.searchByTitlePartial(q));
  }
}