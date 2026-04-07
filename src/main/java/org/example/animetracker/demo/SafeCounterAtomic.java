package org.example.animetracker.demo;

import java.util.concurrent.atomic.AtomicInteger;

public class SafeCounterAtomic implements Counter {
  private final AtomicInteger value = new AtomicInteger(0);

  @Override
  public void increment () {
    value.incrementAndGet();
  }

  @Override
  public int getValue() {
    return value.get();
  }

  @Override
  public void reset() {
    value.set(0);
  }
}
