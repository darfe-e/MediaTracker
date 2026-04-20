package org.example.animetracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OrderBy;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seasons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Season {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private LocalDate releaseDate;
  private Boolean isReleased;
  private Integer totalEpisodes;
  private String format;

  @Column(unique = true)
  private Long externalId;

  @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("number ASC")
  private Set<Episode> episodes = new HashSet<>();

  @ManyToOne
  @JoinColumn(name = "anime_id")
  private Anime anime;

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Season season)) {
      return false;
    }
    return id != null && id.equals(season.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}