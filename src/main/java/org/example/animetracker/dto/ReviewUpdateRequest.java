package org.example.animetracker.dto;

import lombok.Data;

@Data
public class ReviewUpdateRequest {
  private Float assessment;

  private String text;
}