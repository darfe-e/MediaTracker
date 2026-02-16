package org.example.animetracker.controller;

import lombok.AllArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.service.AnimeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}
