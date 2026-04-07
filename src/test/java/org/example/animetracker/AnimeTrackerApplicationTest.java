package org.example.animetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.animetracker.model.ImportTask;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.sql.init.mode=never"
})
class AnimeTrackerApplicationTest {

  @Autowired
  private ApplicationContext context;

  @MockitoBean
  private AnimeRepository animeRepository;

  @MockitoBean
  private AnimeImportService importService;

  @BeforeEach
  void resetMocks() {
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

    verify(importService, times(1))
        .refreshPopularAnimeWithProgress(eq(5), any(ImportTask.class));
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
    // Передаем все необходимые параметры, чтобы Hibernate в методе main
    // инициализировался с H2 диалектом и не искал Postgres
    AnimeTrackerApplication.main(new String[]{
        "--spring.main.web-application-type=none",
        "--spring.datasource.url=jdbc:h2:mem:main_testdb;DB_CLOSE_DELAY=-1",
        "--spring.datasource.driver-class-name=org.h2.Driver",
        "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    });

    assertThat(context.getBean(AnimeTrackerApplication.class)).isNotNull();
  }
}