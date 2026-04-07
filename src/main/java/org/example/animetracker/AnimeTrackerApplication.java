package org.example.animetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.service.AsyncAnimeImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Slf4j
@EnableScheduling
@EnableAsync
@SpringBootApplication
public class AnimeTrackerApplication {

  public static void main(String[] args) {
    SpringApplication.run(AnimeTrackerApplication.class, args);
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  public CommandLineRunner initData(AsyncAnimeImportService asyncImportService,
                                    AnimeRepository animeRepository) {
    return args -> {
      if (animeRepository.count() == 0) {
        String taskId = asyncImportService.startBulkImport(5);
        log.info(">>> Начальный импорт запущен асинхронно. TaskId: {}", taskId);
        log.info(">>> Статус: GET /import/tasks/{}", taskId);
      } else {
        log.info(">>> БД уже заполнена, пропускаем импорт.");
      }
    };
  }
}