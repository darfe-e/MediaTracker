package org.example.mediatracker;

import org.example.animetracker.AnimeTrackerApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;

@SpringBootTest(classes = AnimeTrackerApplication.class)
@ActiveProfiles("test")
class MediaTrackerApplicationTests {

  @Test
  void contextLoads() {
    // This test passes if the Spring application context loads successfully.
  }
}