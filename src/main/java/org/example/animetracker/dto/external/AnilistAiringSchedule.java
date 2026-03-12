package org.example.animetracker.dto.external;

import java.util.List;
import lombok.Data;

@Data
public class AnilistAiringSchedule {
  private List<AnilistAiringScheduleNode> nodes;
}