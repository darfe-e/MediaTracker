package org.example.animetracker.controller;

import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

import org.example.animetracker.dto.ReviewCreateRequest;
import org.example.animetracker.dto.ReviewDto;
import org.example.animetracker.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ReviewControllerTest {

  @Mock private ReviewService reviewService;

  @InjectMocks
  private ReviewController controller;

  private MockMvc mockMvc;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
  }

  @Test
  @DisplayName("GET /users/{userId}/reviews — все отзывы пользователя 200 OK")
  void getAllReviewsByUser_returnsOk() throws Exception {
    when(reviewService.getAllReviewsByUser(1L)).thenReturn(List.of());

    mockMvc.perform(get("/users/1/reviews"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("GET /users/{userId}/reviews/{animeId} — отзыв пользователя 200 OK")
  void getReviewByUserAndAnime_returnsOk() throws Exception {
    ReviewDto dto = new ReviewDto(1L, null, 9.0f, "Great!");
    when(reviewService.getReviewByUserAndAnime(1L, 2L)).thenReturn(dto);

    mockMvc.perform(get("/users/1/reviews/2"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("POST /users/{userId}/reviews — создание отзыва 201 Created")
  void saveReview_returnsCreated() throws Exception {
    ReviewDto dto = new ReviewDto(1L, null, 9.0f, "Amazing!");
    when(reviewService.saveReview(anyLong(), anyLong(), anyFloat(), anyString()))
        .thenReturn(dto);

    ReviewCreateRequest request = new ReviewCreateRequest();
    request.setAnimeId(2L);
    request.setAssessment(9.0f);
    request.setText("Amazing!");

    mockMvc.perform(post("/users/1/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("PUT /users/{userId}/reviews/{animeId} — обновление отзыва 200 OK")
  void updateReview_returnsOk() throws Exception {
    ReviewDto dto = new ReviewDto(1L, null, 8.0f, "Updated!");
    when(reviewService.updateReview(anyLong(), anyLong(), anyFloat(), anyString()))
        .thenReturn(dto);

    ReviewCreateRequest request = new ReviewCreateRequest();
    request.setAnimeId(2L);
    request.setAssessment(8.0f);
    request.setText("Updated!");

    mockMvc.perform(put("/users/1/reviews/2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("DELETE /users/{userId}/reviews/{animeId} — удаление отзыва 204 No Content")
  void deleteReview_returnsNoContent() throws Exception {
    doNothing().when(reviewService).deleteReview(1L, 2L);

    mockMvc.perform(delete("/users/1/reviews/2"))
        .andExpect(status().isNoContent());
  }
}