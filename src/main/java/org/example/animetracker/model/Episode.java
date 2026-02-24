package org.example.animetracker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table (name = "episodes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Episode {
  @Id
  @GeneratedValue (strategy = GenerationType.IDENTITY)
  private Long id;

  private String title;
  private Integer number;
  private LocalDate releaseDate;
  private Boolean isReleased;

  @ManyToOne
  @JoinColumn(name = "season_id", nullable = false)
  private Season season;

}