package org.example.animetracker.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor // Генерирует пустой конструктор автоматически
@Entity
@Table(name = "system_tasks")
public class SystemTask {
  @Id
  private String taskName;
  private LocalDateTime lastRun;

  private Long lastProcessedId;

  public SystemTask(String taskName, LocalDateTime lastRun, Long lastProcessedId) {
    this.taskName = taskName;
    this.lastRun = lastRun;
    this.lastProcessedId = lastProcessedId;
  }

  public SystemTask(String taskName, LocalDateTime lastRun) {
    this.taskName = taskName;
    this.lastRun = lastRun;
    this.lastProcessedId = 0L; // По умолчанию 0
  }

}