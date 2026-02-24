package org.example.animetracker.service;

import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import org.example.animetracker.dto.AnimeUserDetailedDto;
import org.example.animetracker.dto.AnimeUserDto;
import org.example.animetracker.mapper.AnimeUserMapper;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.AnimeUser;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.AnimeUserRepository;
import org.example.animetracker.repository.UserRepository;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AnimeUserService {

  private final AnimeUserRepository animeUserRepository;
  private final AnimeRepository animeRepository;
  private final UserRepository userRepository;

  public List<AnimeUserDto> getByUserIdSortedByAssessment(Long userId) {
    return animeUserRepository.findByUserId(userId).stream()
        .sorted(Comparator.comparing(AnimeUser::getAssessment,
            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .map(AnimeUserMapper::animeUserToDto)
        .toList();
  }

  public AnimeUserDetailedDto getConnection(Long userId, Long animeId) {
    return animeUserRepository.findByUserIdAndAnimeId(userId, animeId)
        .map(AnimeUserMapper::animeUserToDetailedDto)
        .orElse(null);
  }

  public AnimeUserDto addAnimeToCollection(Long animeId, Long userId) {
    Anime anime = animeRepository.findById(animeId).orElse(null);
    User user = userRepository.findById(userId).orElse(null);
    if (anime == null || user == null) {
      return null;
    }
    if (animeUserRepository.findByUserIdAndAnimeId(userId, animeId).isPresent()) {
      return null;
    }
    AnimeUser animeUser = new AnimeUser(user, anime);
    AnimeUser saved = animeUserRepository.save(animeUser);
    return AnimeUserMapper.animeUserToDto(saved);
  }

  public AnimeUserDetailedDto updateUserInfo(
      Long userId, Long animeId, Float assessment, String review) {
    return animeUserRepository.findByUserIdAndAnimeId(userId, animeId)
        .map(animeUser -> {
          if (assessment != null) {
            animeUser.setAssessment(assessment);
          }
          if (review != null) {
            animeUser.setReview(review);
          }

          return AnimeUserMapper.animeUserToDetailedDto(animeUserRepository.save(animeUser));
        })
        .orElse(null);
  }

  public boolean removeConnection(Long userId, Long animeId) {
    return animeUserRepository.findByUserIdAndAnimeId(userId, animeId)
        .map(animeUser -> {
          animeUserRepository.delete(animeUser);
          return true;
        })
        .orElse(false);
  }
}