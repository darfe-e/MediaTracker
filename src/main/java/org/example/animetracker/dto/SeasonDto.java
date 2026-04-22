package org.example.animetracker.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeasonDto {
  private Long id;
  private LocalDate releaseDate;
  private Boolean isReleased;
  private Integer totalEpisodes;
  private String format;
}