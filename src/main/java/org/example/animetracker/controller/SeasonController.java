package org.example.animetracker.controller;

import lombok.RequiredArgsConstructor;
import org.example.animetracker.dto.EpisodeDto;
import org.example.animetracker.service.SeasonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/seasons")
@RequiredArgsConstructor
public class SeasonController {

  private final SeasonService seasonService;

  @GetMapping("/{seasonId}/episodes")
  public ResponseEntity<List<EpisodeDto>> getEpisodes(@PathVariable Long seasonId) {
    return ResponseEntity.ok(seasonService.getEpisodesBySeasonId(seasonId));
  }
}