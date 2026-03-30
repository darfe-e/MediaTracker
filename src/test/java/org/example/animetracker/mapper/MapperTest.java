package org.example.animetracker.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.EpisodeDto;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.dto.ReviewDto;
import org.example.animetracker.dto.SeasonDto;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Episode;
import org.example.animetracker.model.FavoriteAnime;
import org.example.animetracker.model.Review;
import org.example.animetracker.model.Season;
import org.example.animetracker.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MapperTest {

  // ─── AnimeMapper ────────────────────────────────────────────────────────────

  @Test
  @DisplayName("AnimeMapper.animeToDto — null → null")
  void animeToDto_null_returnsNull() {
    assertThat(AnimeMapper.animeToDto(null)).isNull();
  }

  @Test
  @DisplayName("AnimeMapper.animeToDto — объект → AnimeDto")
  void animeToDto_nonNull_returnsDto() {
    Anime anime = buildAnime();
    AnimeDto dto = AnimeMapper.animeToDto(anime);
    assertThat(dto).isNotNull();
    assertThat(dto.getTitle()).isEqualTo("Test");
  }

  @Test
  @DisplayName("AnimeMapper.animeToDetailedDto — null → null")
  void animeToDetailedDto_null_returnsNull() {
    assertThat(AnimeMapper.animeToDetailedDto(null)).isNull();
  }

  @Test
  @DisplayName("AnimeMapper.animeToDetailedDto — объект → AnimeDetailedDto")
  void animeToDetailedDto_nonNull_returnsDto() {
    Anime anime = buildAnime();
    AnimeDetailedDto dto = AnimeMapper.animeToDetailedDto(anime);
    assertThat(dto).isNotNull();
    assertThat(dto.getTitle()).isEqualTo("Test");
  }

  // ─── EpisodeMapper ──────────────────────────────────────────────────────────

  @Test
  @DisplayName("EpisodeMapper.episodeToDto — null → null")
  void episodeToDto_null_returnsNull() {
    assertThat(EpisodeMapper.episodeToDto(null)).isNull();
  }

  @Test
  @DisplayName("EpisodeMapper.episodeToDto — объект → EpisodeDto")
  void episodeToDto_nonNull_returnsDto() {
    Episode episode = new Episode();
    episode.setTitle("Episode 1");
    episode.setNumber(1);
    EpisodeDto dto = EpisodeMapper.episodeToDto(episode);
    assertThat(dto).isNotNull();
    assertThat(dto.getTitle()).isEqualTo("Episode 1");
    assertThat(dto.getNumber()).isEqualTo(1);
  }

  // ─── SeasonMapper ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("SeasonMapper.seasonToDto — null → null")
  void seasonToDto_null_returnsNull() {
    assertThat(SeasonMapper.seasonToDto(null)).isNull();
  }

  @Test
  @DisplayName("SeasonMapper.seasonToDto — объект → SeasonDto")
  void seasonToDto_nonNull_returnsDto() {
    Season season = new Season();
    season.setIsReleased(true);
    SeasonDto dto = SeasonMapper.seasonToDto(season);
    assertThat(dto).isNotNull();
  }

  // ─── FavoriteAnimeMapper ────────────────────────────────────────────────────

  @Test
  @DisplayName("FavoriteAnimeMapper.favoriteAnimeToDto — null → null")
  void favoriteAnimeToDto_null_returnsNull() {
    assertThat(FavoriteAnimeMapper.favoriteAnimeToDto(null)).isNull();
  }

  @Test
  @DisplayName("FavoriteAnimeMapper.favoriteAnimeToDto — объект → FavoriteAnimeDto")
  void favoriteAnimeToDto_nonNull_returnsDto() {
    User user = new User();
    user.setId(1L);
    user.setName("Alice");
    Anime anime = buildAnime();
    FavoriteAnime fav = new FavoriteAnime(user, anime);
    fav.setId(10L);

    FavoriteAnimeDto dto = FavoriteAnimeMapper.favoriteAnimeToDto(fav);
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(10L);
  }

  // ─── ReviewMapper ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("ReviewMapper.reviewToDto — null → null")
  void reviewToDto_null_returnsNull() {
    assertThat(ReviewMapper.reviewToDto(null)).isNull();
  }

  @Test
  @DisplayName("ReviewMapper.reviewToDto — объект → ReviewDto")
  void reviewToDto_nonNull_returnsDto() {
    User user = new User();
    user.setId(1L);
    Anime anime = buildAnime();
    FavoriteAnime fav = new FavoriteAnime(user, anime);
    fav.setId(1L);

    Review review = new Review();
    review.setId(5L);
    review.setFavorite(fav);
    review.setAssessment(8.5f);
    review.setText("Great!");

    ReviewDto dto = ReviewMapper.reviewToDto(review);
    assertThat(dto).isNotNull();
    assertThat(dto.getAssessment()).isEqualTo(8.5f);
  }

  // ─── helpers ────────────────────────────────────────────────────────────────

  private Anime buildAnime() {
    Anime anime = new Anime();
    anime.setId(1L);
    anime.setTitle("Test");
    anime.setNumOfReleasedSeasons(1);
    anime.setStudio("TestStudio");
    anime.setIsOngoing(false);
    return anime;
  }
}