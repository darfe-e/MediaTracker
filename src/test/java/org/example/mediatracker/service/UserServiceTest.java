package org.example.mediatracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.animetracker.dto.UserDto;
import org.example.animetracker.model.User;
import org.example.animetracker.repository.UserRepository;
import org.example.animetracker.service.UserService;
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
  @DisplayName("createUser — успешное создание пользователя")
  void createUser_success_returnsDto() {
    User saved = buildUser(1L, "Alice");
    when(userRepository.save(any(User.class))).thenReturn(saved);

    UserDto result = userService.createUser("Alice");

    assertThat(result).isNotNull();
    assertThat(result.getName()).isEqualTo("Alice");
    assertThat(result.getId()).isEqualTo(1L);
    verify(userRepository).save(any(User.class));
  }

  @Test
  @DisplayName("createUser — имя сохраняется без изменений")
  void createUser_nameIsPreservedExactly() {
    String name = "  SomeUser  ";
    User saved = buildUser(2L, name);
    when(userRepository.save(any(User.class))).thenReturn(saved);

    UserDto result = userService.createUser(name);

    assertThat(result.getName()).isEqualTo(name);
  }

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

  private User buildUser(Long id, String name) {
    User u = new User();
    u.setId(id);
    u.setName(name);
    return u;
  }
}