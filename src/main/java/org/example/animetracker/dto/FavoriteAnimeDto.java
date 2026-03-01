package org.example.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FavoriteAnimeDto {
  private Long id;
  private UserDto user;
  private final AnimeDto anime;
}
