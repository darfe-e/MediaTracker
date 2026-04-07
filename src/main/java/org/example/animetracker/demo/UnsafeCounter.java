package org.example.animetracker.demo;

public class UnsafeCounter implements Counter {
  private int value = 0;

  @Override
  public void increment () {
    value++;
  }

  @Override
  public int getValue () {
    return value;
  }

  @Override
  public void reset () {
    value = 0;
  }
}
