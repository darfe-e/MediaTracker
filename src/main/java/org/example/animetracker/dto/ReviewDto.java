package org.example.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {

  private Long id;
  private FavoriteAnimeDto favorite;
  private Float assessment;
  private String text;
}