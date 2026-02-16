package org.example.animetracker.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeDto {
  private String title;
  private Integer number;
  private LocalDate releaseDate;
  private Boolean isReleased;
}
