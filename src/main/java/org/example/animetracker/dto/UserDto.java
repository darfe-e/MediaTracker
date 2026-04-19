package org.example.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserDto {
  private Long id;
  private String name;
  private String avatarPath;
  private String theme;
}