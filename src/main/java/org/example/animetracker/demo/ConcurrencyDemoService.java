package org.example.animetracker.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ConcurrencyDemoService {

  private static final int THREAD_COUNT = 50;
  private static final int INCREMENTS_PER_THREAD = 1000;
  private static final int EXPECTED_TOTAL = THREAD_COUNT * INCREMENTS_PER_THREAD;

  public int demonstrateRaceCondition() throws InterruptedException {
    Counter counter = new UnsafeCounter();
    runConcurrentIncrements(counter);
    int actual = counter.getValue();
    log.warn("Race condition: expected={}, actual={}, lost updates={}",
        EXPECTED_TOTAL, actual, EXPECTED_TOTAL - actual);
    return actual;
  }

  public int demonstrateSafeSynchronized() throws InterruptedException {
    Counter counter = new SafeCounterSynchronized();
    runConcurrentIncrements(counter);
    int actual = counter.getValue();
    log.info("Safe (synchronized): expected={}, actual={}", EXPECTED_TOTAL, actual);
    return actual;
  }

  public int demonstrateSafeAtomic() throws InterruptedException {
    Counter counter = new SafeCounterAtomic();
    runConcurrentIncrements(counter);
    int actual = counter.getValue();
    log.info("Safe (Atomic): expected={}, actual={}", EXPECTED_TOTAL, actual);
    return actual;
  }

  private void runConcurrentIncrements(Counter counter) throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
    for (int i = 0; i < THREAD_COUNT; i++) {
      executor.submit(() -> {
        for (int j = 0; j < INCREMENTS_PER_THREAD; j++) {
          counter.increment();
        }
      });
    }
    executor.shutdown();
    executor.awaitTermination(1, TimeUnit.MINUTES);
  }
}