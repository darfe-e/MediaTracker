package org.example.animetracker;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.service.AnimeImportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;

@ExtendWith(MockitoExtension.class)
class AnimeTrackerApplicationTest {

  @Mock private AnimeImportService importService;
  @Mock private AnimeRepository animeRepository;

  private final AnimeTrackerApplication app = new AnimeTrackerApplication();

  @Test
  @DisplayName("CommandLineRunner — БД пуста (count=0) → вызывает refreshPopularAnime")
  void initData_databaseEmpty_callsRefresh() throws Exception {
    when(animeRepository.count()).thenReturn(0L);

    CommandLineRunner runner = app.initData(importService, animeRepository);
    runner.run();

    verify(importService).refreshPopularAnime(5);
  }

  @Test
  @DisplayName("CommandLineRunner — БД не пуста (count>0) → импорт пропускается")
  void initData_databaseNotEmpty_skipsImport() throws Exception {
    when(animeRepository.count()).thenReturn(10L);

    CommandLineRunner runner = app.initData(importService, animeRepository);
    runner.run();

    verify(importService, never()).refreshPopularAnime(5);
  }
}