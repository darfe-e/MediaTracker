package org.example.animetracker.service;

import java.util.List;
import lombok.AllArgsConstructor;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.mapper.FavoriteAnimeMapper;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.FavoriteAnime;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.FavoriteAnimeRepository;
import org.example.animetracker.repository.UserRepository;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class FavoriteAnimeService {

  private final FavoriteAnimeRepository favoriteAnimeRepository;
  private final AnimeRepository animeRepository;
  private final UserRepository userRepository;

  public List<FavoriteAnimeDto> getByUserIdSortedByAssessment(Long userId) {
    return favoriteAnimeRepository.findByUserId(userId).stream()
        .map(FavoriteAnimeMapper::favoriteAnimeToDto)
        .toList();
  }

  public FavoriteAnimeDto getConnection(Long userId, Long animeId) {
    FavoriteAnime favorite = favoriteAnimeRepository.findByUserIdAndAnimeId(userId, animeId)
        .orElse(null);
    if (favorite == null) {
      return null;
    }
    return FavoriteAnimeMapper.favoriteAnimeToDto(favorite);
  }

  public FavoriteAnimeDto addAnimeToCollection(Long animeId, Long userId) {
    Anime anime = animeRepository.findById(animeId).orElse(null);
    User user = userRepository.findById(userId).orElse(null);
    if (anime == null || user == null) {
      return null;
    }
    if (favoriteAnimeRepository.findByUserIdAndAnimeId(userId, animeId).isPresent()) {
      return null;
    }
    FavoriteAnime animeUser = new FavoriteAnime(user, anime);
    FavoriteAnime saved = favoriteAnimeRepository.save(animeUser);
    return FavoriteAnimeMapper.favoriteAnimeToDto(saved);
  }

  public boolean removeConnection(Long userId, Long animeId) {
    return favoriteAnimeRepository.findByUserIdAndAnimeId(userId, animeId)
        .map(animeUser -> {
          favoriteAnimeRepository.delete(animeUser);
          return true;
        })
        .orElse(false);
  }
}