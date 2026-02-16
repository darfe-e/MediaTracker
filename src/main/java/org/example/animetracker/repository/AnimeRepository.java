package org.example.animetracker.repository;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Episode;
import org.example.animetracker.model.Season;
import org.springframework.stereotype.Repository;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Repository
public class AnimeRepository {
  private final Map<Long, Anime> animeMap = new HashMap<>();
  private long nextId = 1;

  public void save(Anime anime) {
    if (anime.getId() == null) {
      anime.setId(nextId++);
    }
    animeMap.put(anime.getId(), anime);
  }

  public Anime findById(Long id) {
    return animeMap.get(id);
  }

  public List<Anime> findAll() {
    return new ArrayList<>(animeMap.values());
  }

  public Boolean containsAnime(Long id) {
    return animeMap.containsKey(id);
  }

  @PostConstruct
  public void initData() {
    // ========== Тетрадь смерти ==========
    Anime deathNote = new Anime();
    deathNote.setTitle("Тетрадь смерти");
    deathNote.setStudio("Madhouse");
    deathNote.setNumOfReleasedSeasons(1);
    deathNote.setPopularityRank(1);
    deathNote.setIsOngoing(false);

    Season season1 = new Season();
    season1.setSeasonNumber(1);
    season1.setReleaseDate(LocalDate.of(2006, 10, 3));
    season1.setIsReleased(true);
    season1.setAnime(deathNote); // обратная связь

    // Эпизоды сезона 1
    Episode ep1 = new Episode();
    ep1.setTitle("Перерождение");
    ep1.setNumber(1);
    ep1.setReleaseDate(LocalDate.of(2006, 10, 3));
    ep1.setIsReleased(true);
    ep1.setSeason(season1);

    Episode ep2 = new Episode();
    ep2.setTitle("Обмен");
    ep2.setNumber(2);
    ep2.setReleaseDate(LocalDate.of(2006, 10, 10));
    ep2.setIsReleased(true);
    ep2.setSeason(season1);

    Episode ep3 = new Episode();
    ep3.setTitle("Сделка");
    ep3.setNumber(3);
    ep3.setReleaseDate(LocalDate.of(2006, 10, 17));
    ep3.setIsReleased(true);
    ep3.setSeason(season1);

    season1.setEpisodes(List.of(ep1, ep2, ep3));
    deathNote.setSeasons(List.of(season1));

    save(deathNote);

    // ========== Эхо террора ==========
    Anime terror = new Anime();
    terror.setTitle("Эхо террора");
    terror.setStudio("MAPPA");
    terror.setNumOfReleasedSeasons(1);
    terror.setPopularityRank(50);
    terror.setIsOngoing(false);

    Season seasonT = new Season();
    seasonT.setSeasonNumber(1);
    seasonT.setReleaseDate(LocalDate.of(2014, 7, 10));
    seasonT.setIsReleased(true);
    seasonT.setAnime(terror);

    Episode epT1 = new Episode();
    epT1.setTitle("Падение");
    epT1.setNumber(1);
    epT1.setReleaseDate(LocalDate.of(2014, 7, 10));
    epT1.setIsReleased(true);
    epT1.setSeason(seasonT);

    Episode epT2 = new Episode();
    epT2.setTitle("Взывая");
    epT2.setNumber(2);
    epT2.setReleaseDate(LocalDate.of(2014, 7, 17));
    epT2.setIsReleased(true);
    epT2.setSeason(seasonT);

    seasonT.setEpisodes(List.of(epT1, epT2));
    terror.setSeasons(List.of(seasonT));

    save(terror);

  }

}