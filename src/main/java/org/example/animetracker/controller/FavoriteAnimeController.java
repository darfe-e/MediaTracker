package org.example.animetracker.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.service.FavoriteAnimeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/{userId}/favorites")
@RequiredArgsConstructor
public class FavoriteAnimeController {

  private final FavoriteAnimeService favoriteAnimeService;

  @GetMapping
  public ResponseEntity<Page<AnimeDto>> getAllInCollection(
      @PathVariable Long userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<AnimeDto> collection = favoriteAnimeService
        .getByUserIdSortedByAssessment(userId, pageable);
    return ResponseEntity.ok(collection);
  }

  @GetMapping ("/ongoing")
  public ResponseEntity<Page<AnimeDto>> getAllIsOngoingInCollection(@PathVariable Long userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<AnimeDto> collection = favoriteAnimeService.getOngoingInCollection(userId, pageable);
    return ResponseEntity.ok(collection);
  }

  @GetMapping("/{animeId}")
  public ResponseEntity<AnimeDetailedDto> getConnection(@PathVariable Long userId,
                                                            @PathVariable Long animeId) {
    AnimeDetailedDto dto = favoriteAnimeService.getConnection(userId, animeId);
    if (dto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/{animeId}")
  public ResponseEntity<FavoriteAnimeDto> addAnimeToCollection(@PathVariable Long userId,
                                                               @PathVariable Long animeId) {
    FavoriteAnimeDto dto = favoriteAnimeService.addAnimeToCollection(animeId, userId);
    if (dto == null) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @DeleteMapping("/{animeId}")
  public ResponseEntity<Void> removeConnection(@PathVariable Long userId,
                                               @PathVariable Long animeId) {
    boolean removed = favoriteAnimeService.removeConnection(userId, animeId);
    if (!removed) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}