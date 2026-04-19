package org.example.animetracker.service;

import lombok.AllArgsConstructor;
import org.example.animetracker.dto.UserDto;
import org.example.animetracker.dto.UserRegistrationDto;
import org.example.animetracker.mapper.UserMapper;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@AllArgsConstructor
@Service
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public UserDto registerUser(UserRegistrationDto registrationDto) {
    if (userRepository.findByName(registrationDto.getName()).isPresent()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is already taken");
    }

    User user = new User();
    user.setName(registrationDto.getName());
    user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
    user.setTheme("dark"); // Дефолтное значение

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

  public UserDto findByName(String name) {
    User user = userRepository.findByName(name)
        .orElseThrow(() -> new RuntimeException("User not found: " + name));
    return new UserDto(user.getId(), user.getName(),
        user.getAvatarPath(), user.getTheme());
  }
}