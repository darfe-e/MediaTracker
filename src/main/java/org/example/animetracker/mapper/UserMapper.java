package org.example.animetracker.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.example.animetracker.dto.UserDto;
import org.example.animetracker.model.User;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserMapper {
  public static UserDto userToDto(User user) {
    if (user == null) {
      return null;
    }
    return new UserDto(user.getId(), user.getName());
  }
}