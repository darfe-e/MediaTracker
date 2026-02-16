package org.example.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class AnimeUserDetailedDto {
  private UserDto user;
  private final AnimeDetailedDto anime;
  private Float assessment;
  private String review;
}
