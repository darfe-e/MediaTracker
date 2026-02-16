package org.example.animetracker.model;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Episode {
  private Long id;
  private String title;
  private Integer number;
  private LocalDate releaseDate;
  private Boolean isReleased;
  private Season season;
}