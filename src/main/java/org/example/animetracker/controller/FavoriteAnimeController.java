package org.example.animetracker.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.example.animetracker.controller.api.FavoriteAnimeControllerApi;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.service.FavoriteAnimeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users/{userId}/favorites")
@RequiredArgsConstructor
public class FavoriteAnimeController implements FavoriteAnimeControllerApi {

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
    return ResponseEntity.ok(dto);
  }

  @PostMapping("/{animeId}")
  public ResponseEntity<FavoriteAnimeDto> addAnimeToCollection(@PathVariable Long userId,
                                                               @PathVariable Long animeId) {
    FavoriteAnimeDto dto = favoriteAnimeService.addAnimeToCollection(animeId, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @PostMapping("/bulk")
  public ResponseEntity<List<FavoriteAnimeDto>> addMultipleAnimesToCollection(
      @PathVariable Long userId,
      @RequestBody List<Long> animeIds) {

    List<FavoriteAnimeDto> dtos = favoriteAnimeService
        .addMultipleAnimesToCollectionBulk(userId, animeIds);
    return ResponseEntity.status(HttpStatus.CREATED).body(dtos);
  }

  @PostMapping("/bulk-test-fail")
  public ResponseEntity<List<FavoriteAnimeDto>> addBulkFail(
      @PathVariable Long userId,
      @RequestBody List<Long> ids) {
    return ResponseEntity.ok(favoriteAnimeService.addBulkNonTransactional(userId, ids));
  }

  @DeleteMapping("/{animeId}")
  public ResponseEntity<Void> removeConnection(@PathVariable Long userId,
                                               @PathVariable Long animeId) {
    favoriteAnimeService.removeConnection(userId, animeId);
    return ResponseEntity.noContent().build();
  }
}