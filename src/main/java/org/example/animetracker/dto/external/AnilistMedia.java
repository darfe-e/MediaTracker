package org.example.animetracker.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class AnilistMedia {
  private Long id;
  private Integer idMal;       // MAL ID — используется для запросов к Jikan API
  private AnilistTitle title;
  private String format;
  private String status;
  private Integer episodes;
  private Integer duration;
  private Integer popularity;
  private AnilistDate startDate;
  private AnilistStudios studios;
  private List<String> genres;
  private AnilistRelations relations;
  private AnilistAiringSchedule airingSchedule;
  private AnilistCoverImage coverImage;
}








































