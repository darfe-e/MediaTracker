package org.example.animetracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AnilistRelationEdge {
  private AnilistMedia node;
  private String relationType; // "SEQUEL", "PREQUEL", "SIDE_STORY" и т.д.
}