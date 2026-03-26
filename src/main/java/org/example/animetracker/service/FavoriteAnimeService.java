package org.example.animetracker.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.mapper.AnimeMapper;
import org.example.animetracker.mapper.FavoriteAnimeMapper;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.FavoriteAnime;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.FavoriteAnimeRepository;
import org.example.animetracker.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@AllArgsConstructor
@Service
public class FavoriteAnimeService {

  private final FavoriteAnimeRepository favoriteAnimeRepository;
  private final AnimeRepository animeRepository;
  private final UserRepository userRepository;

  public Page<AnimeDto> getByUserIdSortedByAssessment(Long userId, Pageable pageable) {
    log.info("Getting favorite anime for user {} sorted by assessment, pageable: {}",
        userId, pageable);
    return favoriteAnimeRepository.findAnimeByUserIdSortedByAssessment(userId, pageable)
        .map(AnimeMapper::animeToDto);
  }

  @Transactional(readOnly = true)
  public AnimeDetailedDto getConnection(Long userId, Long animeId) {
    log.debug("Checking favorite connection for user {} and anime {}", userId, animeId);
    FavoriteAnime favorite = favoriteAnimeRepository.findByUserIdAndAnimeId(userId, animeId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Favorite not found for user " + userId + " and anime " + animeId));
    Anime anime = favorite.getAnime();
    log.debug("Found favorite connection for user {} and anime {}", userId, animeId);
    return AnimeMapper.animeToDetailedDto(anime);
  }

  public Page<AnimeDto> getOngoingInCollection(Long userId, Pageable pageable) {
    log.info("Getting ongoing anime for user {} with pageable: {}", userId, pageable);
    Page<Anime> animes = favoriteAnimeRepository
        .getOngoingSortedByAssessment(userId, pageable);
    return animes.map(AnimeMapper::animeToDto);
  }

  @Transactional
  public FavoriteAnimeDto addAnimeToCollection(Long animeId, Long userId) {
    log.info("Adding anime {} to collection of user {}", animeId, userId);
    Anime anime = animeRepository.findById(animeId)
        .orElseThrow(() -> {
          log.error("Anime not found with id: {}", animeId);
          return new ResponseStatusException(HttpStatus.NOT_FOUND,
              "Anime not found with id: " + animeId);
        });
    User user = userRepository.findById(userId)
        .orElseThrow(() -> {
          log.error("User not found with id: {}", userId);
          return new ResponseStatusException(HttpStatus.NOT_FOUND,
              "User not found with id: " + userId);
        });

    if (favoriteAnimeRepository.findByUserIdAndAnimeId(userId, animeId).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Anime already in collection for user " + userId);
    }

    FavoriteAnime favorite = new FavoriteAnime(user, anime);
    FavoriteAnime saved = favoriteAnimeRepository.save(favorite);
    log.info("Successfully added anime {} to collection of user {}, favorite id = {}",
        animeId, userId, saved.getId());
    return FavoriteAnimeMapper.favoriteAnimeToDto(saved);
  }

  public void removeConnection(Long userId, Long animeId) {
    log.info("Removing anime {} from collection of user {}", animeId, userId);
    FavoriteAnime favorite = favoriteAnimeRepository.findByUserIdAndAnimeId(userId, animeId)
        .orElseThrow(() -> {
          log.error("Favorite not found for user {} and anime {}", userId, animeId);
          return new ResponseStatusException(HttpStatus.NOT_FOUND,
              "Favorite not found for user " + userId + " and anime " + animeId);
        });
    favoriteAnimeRepository.delete(favorite);
    log.info("Successfully removed anime {} from collection of user {}", animeId, userId);
  }
}