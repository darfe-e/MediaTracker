package org.example.animetracker.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.AnimeUserDetailedDto;
import org.example.animetracker.dto.AnimeUserDto;
import org.example.animetracker.model.AnimeUser;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AnimeUserMapper {
  public static AnimeUserDto animeUserToDto(AnimeUser animeUser) {
    if (animeUser == null) {
      return null;
    }
    return new AnimeUserDto(
        UserMapper.userToDto(animeUser.getUser()),
        AnimeMapper.animeToDto(animeUser.getAnime()),
        animeUser.getAssessment()
    );
  }

  public static AnimeUserDetailedDto animeUserToDetailedDto(AnimeUser animeUser) {
    if (animeUser == null) {
      return null;
    }
    return new AnimeUserDetailedDto(
        UserMapper.userToDto(animeUser.getUser()),
        AnimeMapper.animeToDetailedDto(animeUser.getAnime()),
        animeUser.getAssessment(),
        animeUser.getReview()
    );
  }
}