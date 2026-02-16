package org.example.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnimeDto {
  private Long id;
  private String title;
  private Integer numOfReleasedSeasons;
  private String studio;
  private Boolean isOngoing;
}
