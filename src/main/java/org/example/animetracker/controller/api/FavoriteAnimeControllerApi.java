package org.example.animetracker.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.dto.FavoriteAnimeDto;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Favorite Anime", description = "Управление избранными аниме пользователя")
public interface FavoriteAnimeControllerApi {

  @Operation(summary = "Получить все избранные аниме пользователя",
      description = "Возвращает страницу аниме с сортировкой по оценке")
  @ApiResponse(responseCode = "200", description = "Список получен")
  @GetMapping
  ResponseEntity<Page<AnimeDto>> getAllInCollection(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size
  );

  @Operation(summary = "Получить только выходящие аниме в избранном",
      description = "Отфильтрованные по статусу 'онгоинг' и отсортированные по оценке")
  @ApiResponse(responseCode = "200", description = "Список получен")
  @GetMapping("/ongoing")
  ResponseEntity<Page<AnimeDto>> getAllIsOngoingInCollection(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  );

  @Operation(summary = "Получить детальную информацию об аниме в избранном")
  @ApiResponse(responseCode = "200", description = "Запись найдена")
  @ApiResponse(responseCode = "404", description = "Запись не найдена")
  @GetMapping("/{animeId}")
  ResponseEntity<AnimeDetailedDto> getConnection(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @Parameter(description = "ID аниме") @PathVariable Long animeId
  );

  @Operation(summary = "Добавить аниме в избранное")
  @ApiResponse(responseCode = "201", description = "Аниме добавлено")
  @ApiResponse(responseCode = "404", description = "Пользователь или аниме не найдены")
  @ApiResponse(responseCode = "409", description = "Аниме уже в избранном")
  @PostMapping("/{animeId}")
  ResponseEntity<FavoriteAnimeDto> addAnimeToCollection(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @Parameter(description = "ID аниме") @PathVariable Long animeId
  );

  @Operation(summary = "Удалить аниме из избранного")
  @ApiResponse(responseCode = "204", description = "Удалено успешно")
  @ApiResponse(responseCode = "404", description = "Запись не найдена")
  @DeleteMapping("/{animeId}")
  ResponseEntity<Void> removeConnection(
      @Parameter(description = "ID пользователя") @PathVariable Long userId,
      @Parameter(description = "ID аниме") @PathVariable Long animeId
  );
}