package org.example.animetracker.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.example.animetracker.dto.ReviewCreateRequest;
import org.example.animetracker.dto.ReviewDto;
import org.example.animetracker.dto.ReviewUpdateRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Reviews", description = "Управление отзывами пользователей")
public interface ReviewControllerApi {

  @Operation(summary = "Получить отзыв пользователя на конкретное аниме")
  @ApiResponse(responseCode = "200", description = "Отзыв найден")
  @ApiResponse(responseCode = "404", description = "Отзыв не найден")
  @GetMapping("/{animeId}")
  ResponseEntity<ReviewDto> getReview(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @Parameter(description = "ID аниме") @PathVariable Long animeId
  );

  @Operation(summary = "Получить все отзывы пользователя")
  @ApiResponse(responseCode = "200", description = "Список получен")
  @GetMapping
  ResponseEntity<List<ReviewDto>> getAllReviews(@Parameter(description = "ID пользователя")
                                                @PathVariable Long userId);

  @Operation(summary = "Создать новый отзыв")
  @ApiResponse(responseCode = "201", description = "Отзыв создан")
  @ApiResponse(responseCode = "404", description = "Связь избранного не найдена")
  @ApiResponse(responseCode = "409", description = "Отзыв уже существует")
  @PostMapping
  ResponseEntity<ReviewDto> saveReview(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @Valid @RequestBody ReviewCreateRequest request
  );

  @Operation(summary = "Обновить существующий отзыв")
  @ApiResponse(responseCode = "200", description = "Отзыв обновлён")
  @ApiResponse(responseCode = "404", description = "Связь избранного или отзыв не найдены")
  @PutMapping("/{animeId}")
  ResponseEntity<ReviewDto> updateReview(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @Parameter(description = "ID аниме") @PathVariable Long animeId,
      @Valid @RequestBody ReviewUpdateRequest request
  );

  @Operation(summary = "Удалить отзыв")
  @ApiResponse(responseCode = "204", description = "Удалено успешно")
  @ApiResponse(responseCode = "404", description = "Связь избранного или отзыв не найдены")
  @DeleteMapping("/{animeId}")
  ResponseEntity<Void> deleteReview(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @Parameter(description = "ID аниме") @PathVariable Long animeId
  );
}