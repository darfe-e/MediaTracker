package org.example.animetracker.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.model.FavoriteAnime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FavoriteAnimeMapper {
  public static FavoriteAnimeDto favoriteAnimeToDto(FavoriteAnime animeUser) {
    if (animeUser == null) {
      return null;
    }
    return new FavoriteAnimeDto(
        animeUser.getId(),
        UserMapper.userToDto(animeUser.getUser()),
        AnimeMapper.animeToDto(animeUser.getAnime())
    );
  }
}