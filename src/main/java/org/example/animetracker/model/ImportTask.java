package org.example.animetracker.model;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportTask {

  public enum Status {
    PENDING,
    IN_PROGRESS,
    DONE,
    FAILED
  }

  private final String id;
  private final String description;
  private volatile Status status;
  private volatile int processedCount;
  private volatile int totalCount;
  private volatile String error;
  private final LocalDateTime createdAt;
  private volatile LocalDateTime startedAt;
  private volatile LocalDateTime completedAt;

  public ImportTask(String id, String description, int totalCount) {
    this.id          = id;
    this.description = description;
    this.totalCount  = totalCount;
    this.status      = Status.PENDING;
    this.createdAt   = LocalDateTime.now();
  }

  public double getProgress() {
    if (totalCount == 0) {
      return 0.0;
    }
    return (double) processedCount / totalCount;
  }
}