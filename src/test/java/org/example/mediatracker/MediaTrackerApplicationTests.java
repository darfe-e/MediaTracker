package org.example.mediatracker;

import org.example.animetracker.AnimeTrackerApplication;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;

@SpringBootTest(classes = AnimeTrackerApplication.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class MediaTrackerApplicationTests {

  @Test
  void contextLoads() {
    // This test passes if the Spring application context loads successfully.
  }
}