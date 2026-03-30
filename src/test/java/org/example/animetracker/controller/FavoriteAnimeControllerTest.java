package org.example.animetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.example.animetracker.service.FavoriteAnimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FavoriteAnimeControllerTest {

  @Mock private FavoriteAnimeService favoriteAnimeService;

  @InjectMocks
  private FavoriteAnimeController controller;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  @DisplayName("GET /users/{userId}/favorites — возвращает коллекцию 200 OK")
  void getAllInCollection_returnsOk() throws Exception {
    when(favoriteAnimeService.getByUserIdSortedByAssessment(anyLong(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/users/1/favorites"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /users/{userId}/favorites/ongoing — возвращает онгоинги 200 OK")
  void getAllIsOngoingInCollection_returnsOk() throws Exception {
    when(favoriteAnimeService.getOngoingInCollection(anyLong(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of()));

    mockMvc.perform(get("/users/1/favorites/ongoing"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /users/{userId}/favorites/{animeId} — возвращает связь 200 OK")
  void getConnection_returnsOk() throws Exception {
    AnimeDetailedDto dto =
        new AnimeDetailedDto(1L, "Naruto", 5, "Pierrot", List.of(), true);
    when(favoriteAnimeService.getConnection(1L, 2L)).thenReturn(dto);

    mockMvc.perform(get("/users/1/favorites/2"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /users/{userId}/favorites/{animeId} — добавляет аниме 201 Created")
  void addAnimeToCollection_returnsCreated() throws Exception {
    AnimeDto animeDto = new AnimeDto(1L, "Naruto", 5, "Pierrot", true);
    FavoriteAnimeDto dto = new FavoriteAnimeDto(10L, null, animeDto);
    when(favoriteAnimeService.addAnimeToCollection(2L, 1L)).thenReturn(dto);

    mockMvc.perform(post("/users/1/favorites/2"))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("POST /users/{userId}/favorites/bulk — пакетное добавление 201 Created")
  void addMultipleAnimesToCollection_returnsCreated() throws Exception {
    AnimeDto animeDto = new AnimeDto(1L, "Naruto", 5, "Pierrot", true);
    FavoriteAnimeDto dto = new FavoriteAnimeDto(10L, null, animeDto);
    when(favoriteAnimeService.addMultipleAnimesToCollectionBulk(anyLong(), any()))
        .thenReturn(List.of(dto));

    mockMvc.perform(post("/users/1/favorites/bulk")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(2L))))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("POST /users/{userId}/favorites/bulk-test-fail — нетранзакционное 200 OK")
  void addBulkFail_returnsOk() throws Exception {
    when(favoriteAnimeService.addBulkNonTransactional(anyLong(), any()))
        .thenReturn(List.of());

    mockMvc.perform(post("/users/1/favorites/bulk-test-fail")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(List.of(2L, 3L))))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("DELETE /users/{userId}/favorites/{animeId} — удаляет связь 204 No Content")
  void removeConnection_returnsNoContent() throws Exception {
    doNothing().when(favoriteAnimeService).removeConnection(1L, 2L);

    mockMvc.perform(delete("/users/1/favorites/2"))
        .andExpect(status().isNoContent());
  }
}