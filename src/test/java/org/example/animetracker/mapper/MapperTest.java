package org.example.animetracker.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.example.animetracker.dto.*;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.Episode;
import org.example.animetracker.model.FavoriteAnime;
import org.example.animetracker.model.Review;
import org.example.animetracker.model.Season;
import org.example.animetracker.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class MapperTest {

  // ─── UserMapper ─────────────────────────────────────────────────────────────

  @Test
  @DisplayName("UserMapper.userToDto — null → null")
  void userToDto_null_returnsNull() {
    assertThat(UserMapper.userToDto(null)).isNull();
  }

  @Test
  @DisplayName("UserMapper.userToDto — объект → UserDto")
  void userToDto_nonNull_returnsDto() {
    // GIVEN
    User user = new User();
    user.setId(1L);
    user.setName("Alice");

    // WHEN
    UserDto dto = UserMapper.userToDto(user);

    // THEN
    assertThat(dto).isNotNull();
    assertThat(dto.getId()).isEqualTo(1L);
    assertThat(dto.getName()).isEqualTo("Alice");
  }
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

  @Test
  @DisplayName("dtoToEpisode — должен корректно переносить все поля")
  void dtoToEpisode_success() {
    // GIVEN
    LocalDate releaseDate = LocalDate.of(2023, 10, 12);
    EpisodeDto dto = new EpisodeDto("Oshi no Ko Episode 1", 1, releaseDate);

    // WHEN
    Episode result = EpisodeMapper.dtoToEpisode(dto);

    // THEN
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo("Oshi no Ko Episode 1");
    assertThat(result.getNumber()).isEqualTo(1);
    assertThat(result.getReleaseDate()).isEqualTo(releaseDate);
  }

  @Test
  @DisplayName("dtoToEpisode — должен возвращать null, если на вход подан null")
  void dtoToEpisode_nullInput() {
    // WHEN
    Episode result = EpisodeMapper.dtoToEpisode(null);

    // THEN
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("dtoToEpisode — должен работать корректно, если поля DTO пусты (null)")
  void dtoToEpisode_nullFields() {
    // GIVEN
    EpisodeDto dto = new EpisodeDto(null, null, null);

    // WHEN
    Episode result = EpisodeMapper.dtoToEpisode(dto);

    // THEN
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isNull();
    assertThat(result.getNumber()).isNull();
    assertThat(result.getReleaseDate()).isNull();
  }

  @Test
  @DisplayName("episodeToDto — должен корректно маппить сущность в DTO")
  void episodeToDto_success() {
    // GIVEN
    Episode episode = new Episode();
    episode.setTitle("Frieren Ep 1");
    episode.setNumber(1);
    episode.setReleaseDate(LocalDate.now());

    // WHEN
    EpisodeDto result = EpisodeMapper.episodeToDto(episode);

    // THEN
    assertThat(result).isNotNull();
    assertThat(result.getTitle()).isEqualTo(episode.getTitle());
    assertThat(result.getNumber()).isEqualTo(episode.getNumber());
    assertThat(result.getReleaseDate()).isEqualTo(episode.getReleaseDate());
  }

  // ─── SeasonMapper ───────────────────────────────────────────────────────────

  @Test
  @DisplayName("SeasonMapper.seasonToDto — episodes is null → возвращает пустой список")
  void seasonToDto_nullEpisodes_returnsEmptyList() {
    // GIVEN
    Season season = new Season();
    season.setEpisodes(null); // Явно ставим null
    season.setIsReleased(true);

    // WHEN
    SeasonDto dto = SeasonMapper.seasonToDto(season);

    // THEN
    assertThat(dto).isNotNull();
    assertThat(dto.getEpisodes()).isNotNull();
    assertThat(dto.getEpisodes()).isEmpty(); // Проверка той самой строчки с new ArrayList<>()
  }

  @Test
  @DisplayName("SeasonMapper.seasonToDto — обычный объект → SeasonDto")
  void seasonToDto_withEpisodes_returnsDto() {
    // GIVEN
    Season season = new Season();
    season.setIsReleased(false);

    // Создаем эпизод для проверки маппинга внутри списка
    Episode episode = new Episode();
    episode.setTitle("Episode 1");
    season.setEpisodes(java.util.Set.of(episode));

    // WHEN
    SeasonDto dto = SeasonMapper.seasonToDto(season);

    // THEN
    assertThat(dto).isNotNull();
    assertThat(dto.getIsReleased()).isFalse();
    assertThat(dto.getEpisodes()).hasSize(1);
    assertThat(dto.getEpisodes().get(0).getTitle()).isEqualTo("Episode 1");
  }
}