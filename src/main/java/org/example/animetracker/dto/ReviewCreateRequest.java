package org.example.animetracker.dto;

import lombok.Data;

@Data
public class ReviewCreateRequest {
  private Long animeId;

  private Float assessment;

  private String text;
}