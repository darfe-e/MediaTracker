package org.example.animetracker.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Anime Catalogue", description = "Управление каталогом аниме")
public interface AnimeControllerApi {

  @Operation(summary = "Получить аниме по ID",
      description = "Возвращает детальную информацию об аниме (сезоны и эпизоды)")
  @ApiResponse(responseCode = "200", description = "Аниме найдено")
  @ApiResponse(responseCode = "404", description = "Аниме не найдено")
  @GetMapping("/{id}")
  ResponseEntity<AnimeDetailedDto> getById(
      @Parameter(description = "ID аниме") @PathVariable Long id);

  @Operation(summary = "Поиск аниме по студии и/или названию (без пагинации)")
  @ApiResponse(responseCode = "200", description = "Поиск выполнен успешно")
  @ApiResponse(responseCode = "404", description = "Аниме не найдены")
  @GetMapping
  ResponseEntity<List<AnimeDto>> getByStudioAndTitle(
      @Parameter(description = "Название студии") @RequestParam(required = false) String studio,
      @Parameter(description = "Название аниме") @RequestParam(required = false) String title
  );

  @Operation(summary = "Получить все аниме, отсортированные по популярности (с пагинацией)")
  @ApiResponse(responseCode = "200", description = "Список отсортирован")
  @GetMapping("/")
  ResponseEntity<Page<AnimeDto>> getAllSorted(
      @Parameter(description = "Номер страницы") @RequestParam(defaultValue = "0") int page,
      @Parameter(description = "Размер страницы") @RequestParam(defaultValue = "10") int size
  );

  @Operation(summary = "Импорт аниме из внешнего API по названию")
  @ApiResponse(responseCode = "200", description = "Аниме найдено и импортировано")
  @ApiResponse(responseCode = "404", description = "Аниме не найдено")
  @GetMapping("/search")
  ResponseEntity<AnimeDto> searchAnime(@Parameter(description = "Название аниме")
                                       @RequestParam String title);

  @Operation(summary = "Сложный поиск (JPQL) с фильтрацией по жанру и "
      + "минимальному числу сезонов (с пагинацией)")
  @ApiResponse(responseCode = "200", description = "Поиск выполнен")
  @GetMapping("/search-jpql")
  ResponseEntity<Page<AnimeDto>> searchJpql(
      @Parameter(description = "Жанр") @RequestParam String genre,
      @Parameter(description = "Минимальное количество сезонов") @RequestParam int minSeasons,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  );

  @Operation(summary = "Сложный поиск (нативный SQL) с фильтрацией по жанру и "
      + "минимальному числу сезонов (с пагинацией)")
  @ApiResponse(responseCode = "200", description = "Поиск выполнен")
  @GetMapping("/search-native")
  ResponseEntity<Page<AnimeDto>> searchNative(
      @Parameter(description = "Жанр") @RequestParam String genre,
      @Parameter(description = "Минимальное количество сезонов") @RequestParam int minSeasons,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  );
}