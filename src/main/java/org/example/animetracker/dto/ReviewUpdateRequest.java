package org.example.animetracker.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewUpdateRequest {
  @Min(value = 0)
  @Max(value = 10)
  private Float assessment;

  @Size(max = 1000)
  private String text;
}