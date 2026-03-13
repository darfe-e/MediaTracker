package org.example.animetracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewCreateRequest {
  @NotNull(message = "Anime ID is required")
  private Long animeId;

  @Min(value = 0, message = "Assessment must be at least 0")
  @Max(value = 10, message = "Assessment must be at most 10")
  private Float assessment;

  @Size(max = 1000, message = "Review text cannot exceed 1000 characters")
  private String text;
}