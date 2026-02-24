package org.example.animetracker.service;

import lombok.AllArgsConstructor;
import org.example.animetracker.dto.UserDto;
import org.example.animetracker.mapper.UserMapper;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.UserRepository;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;

  public UserDto createUser(String name) {
    User user = new User();
    user.setName(name);
    User saved = userRepository.save(user);
    return UserMapper.userToDto(saved);
  }

  public boolean deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
      return false;
    }
    userRepository.deleteById(id);
    return true;
  }
}