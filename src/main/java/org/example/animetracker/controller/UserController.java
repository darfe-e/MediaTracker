package org.example.animetracker.controller;

import lombok.AllArgsConstructor;
import org.example.animetracker.controller.api.UserControllerApi;
import org.example.animetracker.dto.UserDto;
import org.example.animetracker.dto.UserRegistrationDto;
import org.example.animetracker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController implements UserControllerApi {

  private final UserService userService;

  @PostMapping("/register")
  public ResponseEntity<UserDto> registerUser(@RequestBody UserRegistrationDto dto) {
    UserDto created = userService.registerUser(dto);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/login")
  public ResponseEntity<UserDto> loginUser(@RequestBody UserRegistrationDto dto) {
    UserDto found = userService.findByName(dto.getName());
    return ResponseEntity.ok(found);
  }

}