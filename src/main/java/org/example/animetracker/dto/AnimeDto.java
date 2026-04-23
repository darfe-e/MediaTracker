package org.example.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class AnimeDto {
  private Long id;
  private String title;
  private Integer numOfReleasedSeasons;
  private String studio;
  private Boolean isOngoing;
  private Boolean isAnnounced;
  private String posterUrl;
  private LocalDate nextAiringDate;

  public AnimeDto (Long id, String title, Integer numOfReleasedSeasons,
                   String studio, Boolean isOngiong, Boolean isAnnounced, String posterUrl) {
    this.id = id;
    this.title = title;
    this.numOfReleasedSeasons = numOfReleasedSeasons;
    this.studio = studio;
    this.isOngoing = isOngiong;
    this.isAnnounced = isAnnounced;
    this.posterUrl = posterUrl;
  }
}