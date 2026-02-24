package org.example.animetracker.controller;

import java.util.List;
import lombok.AllArgsConstructor;
import org.example.animetracker.dto.AnimeUserDetailedDto;
import org.example.animetracker.dto.AnimeUserDto;
import org.example.animetracker.service.AnimeUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/anime-collection")
public class AnimeUserController {

  private final AnimeUserService userAnimeService;

  @GetMapping
  public ResponseEntity<List<AnimeUserDto>> getAllInCollection(@RequestParam Long userId) {
    List<AnimeUserDto> collection = userAnimeService.getByUserIdSortedByAssessment(userId);
    return ResponseEntity.ok(collection);
  }

  @GetMapping("/{animeId}")
  public ResponseEntity<AnimeUserDetailedDto> getDetailedAnimeInformation(
      @RequestParam Long userId,
      @PathVariable Long animeId) {
    AnimeUserDetailedDto dto = userAnimeService.getConnection(userId, animeId);
    if (dto == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(dto);
  }

  @PostMapping
  public ResponseEntity<AnimeUserDto> putAnimeToCollection(
      @RequestParam Long userId,
      @RequestParam Long animeId) {
    AnimeUserDto created = userAnimeService.addAnimeToCollection(animeId, userId);
    if (created == null) {
      return ResponseEntity.badRequest().build();
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PutMapping("/{animeId}")
  public ResponseEntity<AnimeUserDetailedDto> setInformation(
      @RequestParam Long userId,
      @PathVariable Long animeId,
      @RequestBody AnimeUserDetailedDto dto) {
    if (dto.getUser() != null && !userId.equals(dto.getUser().getId())) {
      return ResponseEntity.badRequest().build();
    }
    if (dto.getAnime() != null && !animeId.equals(dto.getAnime().getId())) {
      return ResponseEntity.badRequest().build();
    }
    Float assessment = dto.getAssessment();
    String review = dto.getReview();
    AnimeUserDetailedDto updated = userAnimeService.updateUserInfo(
        userId, animeId, assessment, review);
    if (updated == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{animeId}")
  public ResponseEntity<Void> deleteConnection(
      @RequestParam Long userId,
      @PathVariable Long animeId) {
    boolean isDeleted = userAnimeService.removeConnection(userId, animeId);
    if (!isDeleted) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.noContent().build();
  }
}