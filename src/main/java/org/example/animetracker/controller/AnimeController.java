package org.example.animetracker.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.mapper.AnimeMapper;
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
public class AnimeController {

  private final AnimeService animeService;
  private final AnimeImportService animeImportService;

  @GetMapping("/{id}")
  public ResponseEntity<AnimeDetailedDto> getById(@PathVariable Long id) {
    AnimeDetailedDto dto = animeService.findByIdWithoutProblem(id);
    if (dto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(dto);
  }

  @GetMapping
  public ResponseEntity<List<AnimeDto>> getByStudioAndTitle(
      @RequestParam(required = false) String studio,
      @RequestParam(required = false) String title) {
    List<AnimeDto> result = animeService.findByStudioAndName(studio, title);
    if (result.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(result);
  }

  @GetMapping("/")
  public ResponseEntity<Page<AnimeDto>> getAllSorted(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<AnimeDto> result = animeService.getAllSortedByPopularity(pageable);
    if (result.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(result);
  }

  @GetMapping("/search")
  public ResponseEntity<AnimeDto> searchAnime(@RequestParam String title) {
    return animeImportService.importFromApi(title)
        .map(anime -> ResponseEntity.ok(AnimeMapper.animeToDto(anime)))
        .orElse(ResponseEntity.notFound().build());
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
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("popularityRank")));
    Page<AnimeDto> result = animeService
        .findByGenreAndMinSeasonsNative(genre, minSeasons, pageable);
    return ResponseEntity.ok(result);
  }
}
