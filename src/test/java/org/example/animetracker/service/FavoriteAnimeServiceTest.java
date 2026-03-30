package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.FavoriteAnime;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.FavoriteAnimeRepository;
import org.example.animetracker.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FavoriteAnimeServiceTest {

  @Mock private FavoriteAnimeRepository favoriteAnimeRepository;
  @Mock private AnimeRepository         animeRepository;
  @Mock private UserRepository          userRepository;

  @InjectMocks
  private FavoriteAnimeService favoriteAnimeService;

  @Test
  @DisplayName("getByUserIdSortedByAssessment — возвращает страницу DTO")
  void getByUserIdSortedByAssessment_returnsPage() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Anime> animePage = new PageImpl<>(List.of(buildAnime(1L)));
    when(favoriteAnimeRepository.findAnimeByUserIdSortedByAssessment(1L, pageable))
        .thenReturn(animePage);

    Page<AnimeDto> result =
        favoriteAnimeService.getByUserIdSortedByAssessment(1L, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
    verify(favoriteAnimeRepository).findAnimeByUserIdSortedByAssessment(1L, pageable);
  }

  @Test
  @DisplayName("getConnection — связь найдена → возвращает DetailedDto")
  void getConnection_found_returnsDetailedDto() {
    FavoriteAnime favorite = buildFavorite(1L, buildUser(10L), buildAnime(20L));
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(10L, 20L))
        .thenReturn(Optional.of(favorite));

    AnimeDetailedDto result = favoriteAnimeService.getConnection(10L, 20L);

    assertThat(result).isNotNull();
  }

  @Test
  @DisplayName("getConnection — связь не найдена → 404")
  void getConnection_notFound_throws404() {
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> favoriteAnimeService.getConnection(1L, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Favorite not found for user 1");
  }

  @Test
  @DisplayName("getOngoingInCollection — возвращает только онгоинги")
  void getOngoingInCollection_returnsOngoingAnimes() {
    Pageable pageable = PageRequest.of(0, 5);
    Page<Anime> page = new PageImpl<>(List.of(buildAnime(3L)));
    when(favoriteAnimeRepository.getOngoingSortedByAssessment(5L, pageable)).thenReturn(page);

    Page<AnimeDto> result = favoriteAnimeService.getOngoingInCollection(5L, pageable);

    assertThat(result.getTotalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("addAnimeToCollection — успешное добавление")
  void addAnimeToCollection_success_returnsDto() {
    Anime anime = buildAnime(1L);
    User  user  = buildUser(2L);
    FavoriteAnime saved = buildFavorite(10L, user, anime);

    when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));
    when(userRepository.findById(2L)).thenReturn(Optional.of(user));
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(2L, 1L)).thenReturn(Optional.empty());
    when(favoriteAnimeRepository.save(any(FavoriteAnime.class))).thenReturn(saved);

    FavoriteAnimeDto result = favoriteAnimeService.addAnimeToCollection(1L, 2L);

    assertThat(result).isNotNull();
    verify(favoriteAnimeRepository).save(any(FavoriteAnime.class));
  }

  @Test
  @DisplayName("addAnimeToCollection — аниме не найдено → 404")
  void addAnimeToCollection_animeNotFound_throws404() {
    when(animeRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> favoriteAnimeService.addAnimeToCollection(99L, 1L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Anime not found with id: 99");
  }

  @Test
  @DisplayName("addAnimeToCollection — пользователь не найден → 404")
  void addAnimeToCollection_userNotFound_throws404() {
    when(animeRepository.findById(1L)).thenReturn(Optional.of(buildAnime(1L)));
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> favoriteAnimeService.addAnimeToCollection(1L, 99L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("User not found with id: 99");
  }

  @Test
  @DisplayName("addAnimeToCollection — аниме уже в коллекции → 409 Conflict")
  void addAnimeToCollection_duplicate_throws409() {
    Anime anime = buildAnime(1L);
    User  user  = buildUser(2L);

    when(animeRepository.findById(1L)).thenReturn(Optional.of(anime));
    when(userRepository.findById(2L)).thenReturn(Optional.of(user));
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(2L, 1L))
        .thenReturn(Optional.of(buildFavorite(5L, user, anime)));

    assertThatThrownBy(() -> favoriteAnimeService.addAnimeToCollection(1L, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  @DisplayName("removeConnection — успешное удаление")
  void removeConnection_success() {
    FavoriteAnime fav = buildFavorite(7L, buildUser(1L), buildAnime(2L));
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L)).thenReturn(Optional.of(fav));

    favoriteAnimeService.removeConnection(1L, 2L);

    verify(favoriteAnimeRepository).delete(fav);
  }

  @Test
  @DisplayName("removeConnection — запись не найдена → 404")
  void removeConnection_notFound_throws404() {
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 2L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> favoriteAnimeService.removeConnection(1L, 2L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Favorite not found for user 1");
  }

  @Test
  @DisplayName("addMultipleAnimesToCollectionBulk — успех")
  void addMultipleAnimesToCollectionBulk_success() {
    User  user   = buildUser(1L);
    Anime anime1 = buildAnime(10L);
    Anime anime2 = buildAnime(11L);
    FavoriteAnime fav1 = buildFavorite(100L, user, anime1);
    FavoriteAnime fav2 = buildFavorite(101L, user, anime2);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(animeRepository.findById(10L)).thenReturn(Optional.of(anime1));
    when(animeRepository.findById(11L)).thenReturn(Optional.of(anime2));
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 10L)).thenReturn(Optional.empty());
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 11L)).thenReturn(Optional.empty());
    when(favoriteAnimeRepository.saveAll(any())).thenReturn(List.of(fav1, fav2));

    List<FavoriteAnimeDto> result =
        favoriteAnimeService.addMultipleAnimesToCollectionBulk(1L, List.of(10L, 11L));

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("addMultipleAnimesToCollectionBulk — пользователь не найден → 404")
  void addMultipleAnimesToCollectionBulk_userNotFound_throws404() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    List<Long> animeIds = List.of(1L);
    Long userId = 99L;

    assertThatThrownBy(() -> favoriteAnimeService.addMultipleAnimesToCollectionBulk(userId, animeIds))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  @DisplayName("addMultipleAnimesToCollectionBulk — дубликат → 409")
  void addMultipleAnimesToCollectionBulk_duplicate_throws409() {
    User  user  = buildUser(1L);
    Anime anime = buildAnime(5L);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(animeRepository.findById(5L)).thenReturn(Optional.of(anime));
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 5L))
        .thenReturn(Optional.of(buildFavorite(20L, user, anime)));

    List<Long> animeIds = List.of(5L);
    Long userId = 1L;

    assertThatThrownBy(() -> favoriteAnimeService.addMultipleAnimesToCollectionBulk(userId, animeIds))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  @DisplayName("addMultipleAnimesToCollectionBulk — null список → пустой результат")
  void addMultipleAnimesToCollectionBulk_nullList_returnsEmpty() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
    when(favoriteAnimeRepository.saveAll(any())).thenReturn(List.of());

    List<FavoriteAnimeDto> result =
        favoriteAnimeService.addMultipleAnimesToCollectionBulk(1L, null);

    assertThat(result).isEmpty();
    verify(animeRepository, never()).findById(any());
  }

  @Test
  @DisplayName("addBulkNonTransactional — успех: каждое аниме сохраняется по отдельности")
  void addBulkNonTransactional_success() {
    User  user  = buildUser(1L);
    Anime anime = buildAnime(3L);
    FavoriteAnime saved = buildFavorite(50L, user, anime);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(animeRepository.findById(3L)).thenReturn(Optional.of(anime));
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 3L)).thenReturn(Optional.empty());
    when(favoriteAnimeRepository.save(any())).thenReturn(saved);

    List<FavoriteAnimeDto> result =
        favoriteAnimeService.addBulkNonTransactional(1L, List.of(3L));

    assertThat(result).hasSize(1);
    verify(favoriteAnimeRepository).save(any());
  }

  @Test
  @DisplayName("addBulkNonTransactional — дубликат → 409")
  void addBulkNonTransactional_duplicate_throws409() {
    User  user  = buildUser(1L);
    Anime anime = buildAnime(3L);

    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(animeRepository.findById(3L)).thenReturn(Optional.of(anime));
    when(favoriteAnimeRepository.findByUserIdAndAnimeId(1L, 3L))
        .thenReturn(Optional.of(buildFavorite(50L, user, anime)));

    List<Long> animeIds = List.of(3L);
    Long userId = 1L;

    assertThatThrownBy(() -> favoriteAnimeService.addBulkNonTransactional(userId, animeIds))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.CONFLICT));
  }

  @Test
  @DisplayName("addBulkNonTransactional — аниме не найдено → 404")
  void addBulkNonTransactional_animeNotFound_throws404() {
    when(userRepository.findById(1L)).thenReturn(Optional.of(buildUser(1L)));
    when(animeRepository.findById(99L)).thenReturn(Optional.empty());

    List<Long> animeIds = List.of(99L);
    Long userId = 1L;

    assertThatThrownBy(() -> favoriteAnimeService.addBulkNonTransactional(userId, animeIds))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Anime not found: 99");
  }

  private Anime buildAnime(Long id) {
    Anime a = new Anime();
    a.setId(id);
    a.setTitle("Anime#" + id);
    return a;
  }

  private User buildUser(Long id) {
    User u = new User();
    u.setId(id);
    u.setName("User#" + id);
    return u;
  }

  private FavoriteAnime buildFavorite(Long id, User user, Anime anime) {
    FavoriteAnime f = new FavoriteAnime(user, anime);
    f.setId(id);
    return f;
  }
}