package org.example.animetracker.dto.external;

import lombok.Data;

@Data
public class AnilistAiringScheduleNode {
  private Long airingAt; // Время в секундах (Unix timestamp)
  private Integer episode;
}