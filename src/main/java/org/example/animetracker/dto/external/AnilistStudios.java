package org.example.animetracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnilistStudios {
  private List<AnilistStudioEdge> edges; // ВАЖНО: тут теперь edges!
}