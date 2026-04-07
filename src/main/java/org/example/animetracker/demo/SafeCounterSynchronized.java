package org.example.animetracker.demo;

public class SafeCounterSynchronized implements Counter {
  private int value = 0;

  @Override
  public synchronized void increment () {
    value++;
  }

  @Override
  public synchronized int getValue () {
    return value;
  }

  @Override
  public synchronized void reset () {
    value = 0;
  }
}
