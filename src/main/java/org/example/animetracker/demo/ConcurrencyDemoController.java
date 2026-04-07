package org.example.animetracker.demo;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/demo/concurrency")
@RequiredArgsConstructor
public class ConcurrencyDemoController {

  private final ConcurrencyDemoService demoService;
  private static final String EXPECTED = "expected";
  private static final String ACTUAL = "actual";

  @GetMapping("/race")
  public Map<String, Object> raceCondition() throws InterruptedException {
    int actual = demoService.demonstrateRaceCondition();
    return Map.of(
        EXPECTED, 50_000,
        ACTUAL, actual,
        "lostUpdates", 50_000 - actual,
        "message", "Race condition detected! Use /safe or /atomic"
    );
  }

  @GetMapping("/safe")
  public Map<String, Object> safeSynchronized() throws InterruptedException {
    int actual = demoService.demonstrateSafeSynchronized();
    return Map.of(
        EXPECTED, 50_000,
        ACTUAL, actual,
        "correct", actual == 50_000
    );
  }

  @GetMapping("/atomic")
  public Map<String, Object> safeAtomic() throws InterruptedException {
    int actual = demoService.demonstrateSafeAtomic();
    return Map.of(
        EXPECTED, 50_000,
        ACTUAL, actual,
        "correct", actual == 50_000
    );
  }
}