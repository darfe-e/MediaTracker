package org.example.animetracker.service;

import lombok.AllArgsConstructor;
import org.example.animetracker.dto.UserDto;
import org.example.animetracker.mapper.UserMapper;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

  public void deleteUser(Long id) {
    if (!userRepository.existsById(id)) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "user is not found by id: " + id);
    }
    userRepository.deleteById(id);
  }
}