package org.example.animetracker.repository;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.animetracker.model.AnimeUser;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Data
@NoArgsConstructor
@Repository
public class UserAnimeRepository {
  private List<AnimeUser> animeUserList = new ArrayList<>();
  private UserRepository userRepository;
  private AnimeRepository animeRepository;

  public AnimeUser saveConnection(Long animeId, Long userId) {
    if (animeId == null || userId == null) {
      return null;
    }
    if (userRepository.containsUser(userId) && animeRepository.containsAnime(animeId)) {
      AnimeUser animeUser = new AnimeUser(userRepository.findById(userId),
          animeRepository.findById(animeId));
      animeUserList.add(animeUser);
      return animeUser;
    }
    return null;
  }

  public List<AnimeUser> getByUserId(Long userId) {
    List<AnimeUser> animeUserById = new ArrayList<>();
    for (AnimeUser animeUser : animeUserList) {
      if (userId.equals(animeUser.getUser().getId())) {
        animeUserById.add(animeUser);
      }
    }
    return animeUserById;
  }

  public AnimeUser getConnection(Long userId, Long animeId) {
    for (AnimeUser animeUser : animeUserList) {
      if (userId.equals(animeUser.getUser().getId())
          && animeId.equals(animeUser.getAnime().getId())) {
        return animeUser;
      }
    }
    return null;
  }

  public void deleteConnection(AnimeUser animeUser) {

    animeUserList.remove(animeUser);
  }
}
