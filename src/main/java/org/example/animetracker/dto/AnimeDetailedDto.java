package org.example.animetracker.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnimeDetailedDto {
  private Long id;
  private String title;
  private Integer numOfReleasedSeasons;
  private String studio;
  private List<SeasonDto> seasons;
  private Boolean isOngoing;
}
