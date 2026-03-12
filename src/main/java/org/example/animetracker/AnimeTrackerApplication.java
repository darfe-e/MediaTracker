package org.example.animetracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.service.AnimeImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Slf4j
@EnableScheduling
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
  public CommandLineRunner initData(AnimeImportService importService,
                                    AnimeRepository animeRepository) {
    return args -> {
      if (animeRepository.count() == 0) {
        importService.refreshPopularAnime(5);
        log.info(">>> Начальный импорт завершен!");
      } else {
        log.info(">>> БД уже заполнена, пропускаем импорт.");
      }
    };
  }
}