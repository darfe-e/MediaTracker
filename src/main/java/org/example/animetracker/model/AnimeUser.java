package org.example.animetracker.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnimeUser {
  private User user;
  private final Anime anime;
  private Float assessment;
  private String review;

  public AnimeUser(User user, Anime anime) {
    this.anime = anime;
    this.user = user;
  }
}
