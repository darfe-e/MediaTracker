package org.example.animetracker.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Anime {
  private Long id;
  private String title;
  private Integer numOfReleasedSeasons;
  private String studio;
  private List<Season> seasons = new ArrayList<>();
  private Integer popularityRank;
  private Boolean isOngoing;

  public Anime(String title, Integer numOfReleasedSeasons, String studio,
               List<Season> seasons, Integer popularityRank) {
    this.title = title;
    this.studio = studio;
    this.numOfReleasedSeasons = numOfReleasedSeasons;
    this.seasons = seasons;
    this.popularityRank = popularityRank;
  }
}
