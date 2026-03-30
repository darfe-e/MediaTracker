package org.example.animetracker.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.model.Anime;
import org.example.animetracker.service.AnimeImportService;
import org.example.animetracker.service.AnimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AnimeControllerTest {

  @Mock private AnimeService animeService;
  @Mock private AnimeImportService animeImportService;

  @InjectMocks
  private AnimeController controller;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    mockMvc = MockMvcBuilders.standaloneSetup(controller)
        .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
        .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
        .build();
  }

  @Test
  @DisplayName("GET /{id} — возвращает 200 OK")
  void getById_returnsOk() throws Exception {
    AnimeDetailedDto dto =
        new AnimeDetailedDto(1L, "Naruto", 5, "Studio Pierrot", List.of(), true);
    when(animeService.findByIdWithoutProblem(1L)).thenReturn(dto);

    mockMvc.perform(get("/anime-catalogue/1"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /?studio&title — возвращает 200 OK")
  void getByStudioAndTitle_withParams_returnsOk() throws Exception {
    when(animeService.findByStudioAndName(anyString(), anyString()))
        .thenReturn(List.of(new AnimeDto(1L, "Naruto", 5, "Pierrot", true)));

    mockMvc.perform(get("/anime-catalogue")
            .param("studio", "Pierrot")
            .param("title", "Naruto"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET / (без параметров) — возвращает 200 OK")
  void getByStudioAndTitle_noParams_returnsOk() throws Exception {
    when(animeService.findByStudioAndName(null, null)).thenReturn(List.of());

    mockMvc.perform(get("/anime-catalogue"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET // — возвращает страницу 200 OK")
  void getAllSorted_returnsOk() throws Exception {
    // Используем PageRequest, чтобы избежать UnsupportedOperationException
    // при Jackson-сериализации Pageable.unpaged()
    when(animeService.getAllSortedByPopularity(any(Pageable.class)))
        .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0));

    mockMvc.perform(get("/anime-catalogue/")
            .param("page", "0").param("size", "10"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /search — аниме найдено → 200 OK")
  void searchAnime_found_returnsOk() throws Exception {
    Anime anime = new Anime();
    anime.setId(1L);
    anime.setTitle("Naruto");
    when(animeImportService.importFromApi("Naruto")).thenReturn(Optional.of(anime));

    mockMvc.perform(get("/anime-catalogue/search").param("title", "Naruto"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /search — аниме не найдено → 404")
  void searchAnime_notFound_returns404() throws Exception {
    when(animeImportService.importFromApi("Unknown")).thenReturn(Optional.empty());

    mockMvc.perform(get("/anime-catalogue/search").param("title", "Unknown"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /search-jpql — возвращает 200 OK")
  void searchJpql_returnsOk() throws Exception {
    when(animeService.findByGenreAndMinSeasons(anyString(), anyInt(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0));

    mockMvc.perform(get("/anime-catalogue/search-jpql")
            .param("genre", "Action").param("minSeasons", "1"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /search-native — возвращает 200 OK")
  void searchNative_returnsOk() throws Exception {
    when(animeService.findByGenreAndMinSeasonsNative(anyString(), anyInt(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(new ArrayList<>(), PageRequest.of(0, 10), 0));

    mockMvc.perform(get("/anime-catalogue/search-native")
            .param("genre", "Action").param("minSeasons", "1"))
        .andExpect(status().isOk());
  }
}