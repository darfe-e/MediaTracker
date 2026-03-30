package org.example.animetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.service.AnimeImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.ApplicationContext;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// Используем RANDOM_PORT, чтобы не было конфликтов по портам
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AnimeTrackerApplicationTest {

  @Autowired
  private ApplicationContext context;

  @MockitoBean
  private AnimeRepository animeRepository;

  @MockitoBean
  private AnimeImportService importService;

  @BeforeEach
  void resetMocks() {
    // Сбрасываем моки перед каждым тестом,
    // так как CommandLineRunner уже мог дернуть их при старте контекста
    Mockito.reset(animeRepository, importService);
  }

  @Test
  @DisplayName("Context — проверка создания бинов конфигурации")
  void contextLoads() {
    assertThat(context.getBean(RestTemplate.class)).isNotNull();
    assertThat(context.getBean(ObjectMapper.class)).isNotNull();
    assertThat(context.getBean(AnimeTrackerApplication.class)).isNotNull();
  }

  @Test
  @DisplayName("initData — если БД пуста, должен вызываться refreshPopularAnime")
  void initData_whenDbEmpty_callsImport() throws Exception {
    when(animeRepository.count()).thenReturn(0L);
    CommandLineRunner runner = context.getBean(CommandLineRunner.class);

    runner.run();

    verify(animeRepository, atLeastOnce()).count();
    verify(importService, times(1)).refreshPopularAnime(5);
  }

  @Test
  @DisplayName("initData — если БД заполнена, импорт должен быть пропущен")
  void initData_whenDbNotEmpty_skipsImport() throws Exception {
    when(animeRepository.count()).thenReturn(10L);
    CommandLineRunner runner = context.getBean(CommandLineRunner.class);

    runner.run();

    verify(animeRepository, atLeastOnce()).count();
    verify(importService, never()).refreshPopularAnime(anyInt());
  }

  @Test
  @DisplayName("main — проверка запуска приложения")
  void mainMethodTest() {
    // Чтобы main не пытался занять порт 8080, запускаем его без веб-сервера
    AnimeTrackerApplication.main(new String[]{"--spring.main.web-application-type=none"});

    assertThat(context.getBean(AnimeTrackerApplication.class)).isNotNull();
  }
}