package org.example.animetracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AnilistData {
  private AnilistMedia media;
  private AnilistPage page;
}

