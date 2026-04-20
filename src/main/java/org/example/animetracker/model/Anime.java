package org.example.animetracker.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OrderBy;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "animes")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Anime {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String title;
  private Integer numOfReleasedSeasons;
  private String studio;
  private LocalDateTime lastUpdated;
  private Integer duration;

  @Column(unique = true)
  private Long externalId;

  @Column(name = "poster_url")
  private String posterUrl;

  @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL)
  @OrderBy("number ASC")
  private Set<Season> seasons = new HashSet<>();

  private Integer popularityRank;
  private Boolean isOngoing;
  private Boolean isAnnounced;

  @ManyToMany
  @JoinTable(
      name = "anime_genre",
      joinColumns = @JoinColumn(name = "anime_id"),
      inverseJoinColumns = @JoinColumn(name = "genre_id")
  )
  private Set<Genre> genres = new HashSet<>();

  @OneToMany(mappedBy = "anime", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<FavoriteAnime> animeUsers = new HashSet<>();

  public Anime(String title, Integer numOfReleasedSeasons, String studio,
               Set<Season> seasons, Integer popularityRank) {
    this.title = title;
    this.studio = studio;
    this.numOfReleasedSeasons = numOfReleasedSeasons;
    this.seasons = seasons;
    this.popularityRank = popularityRank;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Anime anime)) {
      return false;
    }
    return id != null && id.equals(anime.getId());
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
