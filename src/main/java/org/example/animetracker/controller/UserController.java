package org.example.animetracker.controller;

import lombok.AllArgsConstructor;
import org.example.animetracker.controller.api.UserControllerApi;
import org.example.animetracker.dto.UserDto;
import org.example.animetracker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController implements UserControllerApi {

  private final UserService userService;

  @PostMapping
  public ResponseEntity<UserDto> createUser(@RequestParam String name) {
    UserDto created = userService.createUser(name);
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
    userService.deleteUser(id);
    return ResponseEntity.noContent().build();
  }
}