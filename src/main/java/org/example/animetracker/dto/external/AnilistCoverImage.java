package org.example.animetracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AnilistCoverImage {
  private String extraLarge;
  private String large;
  private String medium;
}