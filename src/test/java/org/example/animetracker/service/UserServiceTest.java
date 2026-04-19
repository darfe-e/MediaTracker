package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.animetracker.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  @Test
  @DisplayName("deleteUser — пользователь существует → успешное удаление")
  void deleteUser_userExists_deletedSuccessfully() {
    when(userRepository.existsById(10L)).thenReturn(true);

    userService.deleteUser(10L);

    verify(userRepository).deleteById(10L);
  }

  @Test
  @DisplayName("deleteUser — пользователь не найден → 404, deleteById не вызывается")
  void deleteUser_userNotFound_throws404() {
    when(userRepository.existsById(99L)).thenReturn(false);

    assertThatThrownBy(() -> userService.deleteUser(99L))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("user is not found by id: 99");

    verify(userRepository, never()).deleteById(any());
  }
}