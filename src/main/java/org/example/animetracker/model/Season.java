package org.example.animetracker.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Season {
  private Long id;
  private Integer seasonNumber;
  private LocalDate releaseDate;
  private Boolean isReleased;
  private List<Episode> episodes = new ArrayList<>();
  private Anime anime;
}
