package org.example.animetracker.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.example.animetracker.dto.AnimeUserDetailedDto;
import org.example.animetracker.dto.AnimeUserDto;
import org.example.animetracker.mapper.AnimeUserMapper;
import org.example.animetracker.model.AnimeUser;
import org.example.animetracker.repository.UserAnimeRepository;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UserAnimeService {

  private final UserAnimeRepository userAnimeRepository;

  public List<AnimeUserDto> getByUserIdSortedByAssessment(Long userId) {
    return userAnimeRepository.getByUserId(userId).stream()
        .sorted(Comparator.comparing(AnimeUser::getAssessment,
            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
        .map(AnimeUserMapper::animeUserToDto)
        .collect(Collectors.toList());
  }

  public AnimeUserDetailedDto getConnection(Long userId, Long animeId) {
    AnimeUser animeUser = userAnimeRepository.getConnection(userId, animeId);
    if (animeUser == null) {
      return null;
    }
    return AnimeUserMapper.animeUserToDetailedDto(animeUser);
  }

  public AnimeUserDto save(Long animeId, Long userId) {
    AnimeUser animeUser = userAnimeRepository.saveConnection(animeId, userId);
    return AnimeUserMapper.animeUserToDto(animeUser);
  }

  public AnimeUserDetailedDto setUserInformation(
      Long userId, Long animeId, AnimeUserDetailedDto dto) {
    AnimeUser animeUser = userAnimeRepository.getConnection(userId, animeId);
    if (animeUser == null) {
      return null;
    }
    if (dto.getAssessment() != null) {
      animeUser.setAssessment(dto.getAssessment());
    }
    if (dto.getReview() != null) {
      animeUser.setReview(dto.getReview());
    }
    return AnimeUserMapper.animeUserToDetailedDto(animeUser);
  }

  public boolean remove(Long userId, Long animeId) {
    AnimeUser animeUser = userAnimeRepository.getConnection(userId, animeId);
    if (animeUser == null) {
      return false;
    }
    userAnimeRepository.deleteConnection(animeUser);
    return true;
  }
}