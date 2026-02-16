package org.example.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AnimeUserDto {
  private UserDto user;
  private final AnimeDto anime;
  private Float assessment;
}
