package org.example.animetracker.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.service.AnimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/anime-catalogue")
public class AnimeController {

  private final AnimeService animeService;

  @GetMapping("/{id}")
  public ResponseEntity<AnimeDetailedDto> getById(@PathVariable Long id) {
    AnimeDetailedDto dto = animeService.findById(id);
    if (dto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(dto);
  }

  @GetMapping("/without-probl/{id}")
  public ResponseEntity<AnimeDetailedDto> getByIdWithoutProblem(@PathVariable Long id) {
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
  public ResponseEntity<List<AnimeDto>> getAllSorted() {
    List<AnimeDto> result = animeService.getAllSortedByPopularity();
    if (result.isEmpty()) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(result);
  }

  @PostMapping("/add-anime")
  public ResponseEntity<String> addAnimeWithTransaction(@RequestBody AnimeDetailedDto dto) {
    try {
      animeService.createAnimeWithSeasonsWithTransaction(dto);
      return ResponseEntity.ok("Аниме успешно добавлено");
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Ошибка при добавлении: " + e.getMessage());
    }
  }

  @PostMapping("/add-anime-no-tx")
  public ResponseEntity<String> addAnimeWithoutTransaction(@RequestBody AnimeDetailedDto dto) {
    try {
      animeService.createAnimeWithSeasonsWithoutTransaction(dto);
      return ResponseEntity.ok("Аниме успешно добавлено (без транзакции)");
    } catch (RuntimeException e) {
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Ошибка при добавлении: " + e.getMessage());
    }
  }
}
