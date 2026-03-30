package org.example.animetracker.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.example.animetracker.dto.UserDto;
import org.example.animetracker.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

  @Mock private UserService userService;

  @InjectMocks
  private UserController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  @DisplayName("POST /users?name — создание пользователя 201 Created")
  void createUser_returnsCreated() throws Exception {
    UserDto dto = new UserDto(1L, "Alice");
    when(userService.createUser(anyString())).thenReturn(dto);

    mockMvc.perform(post("/users").param("name", "Alice"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("DELETE /users/{id} — удаление пользователя 204 No Content")
  void deleteUser_returnsNoContent() throws Exception {
    doNothing().when(userService).deleteUser(1L);

    mockMvc.perform(delete("/users/1"))
        .andExpect(status().isNoContent());
  }
}