package org.example.animetracker.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.service.FavoriteAnimeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/favorites")
@RequiredArgsConstructor
public class FavoriteAnimeController {

  private final FavoriteAnimeService favoriteAnimeService;

  @GetMapping
  public ResponseEntity<List<FavoriteAnimeDto>> getAllInCollection(@PathVariable Long userId) {
    List<FavoriteAnimeDto> collection = favoriteAnimeService.getByUserIdSortedByAssessment(userId);
    return ResponseEntity.ok(collection);
  }

  @GetMapping("/{animeId}")
  public ResponseEntity<FavoriteAnimeDto> getConnection(@PathVariable Long userId,
                                                        @PathVariable Long animeId) {
    FavoriteAnimeDto dto = favoriteAnimeService.getConnection(userId, animeId);
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