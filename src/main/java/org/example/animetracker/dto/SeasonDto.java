package org.example.animetracker.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeasonDto {
  private LocalDate releaseDate;
  private Boolean isReleased;
  private List<EpisodeDto> episodes = new ArrayList<>();
}