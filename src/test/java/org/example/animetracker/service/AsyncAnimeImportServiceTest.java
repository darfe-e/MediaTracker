package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import org.example.animetracker.model.ImportTask;
import org.example.animetracker.model.ImportTask.Status;
import org.example.animetracker.service.worker.AnimeImportWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncAnimeImportServiceTest {

  @Mock
  private AnimeImportWorker importWorker;

  @InjectMocks
  private AsyncAnimeImportService asyncService;

  @Test
  void startBulkImport_shouldReturnTaskIdAndDelegateToWorker() {
    String taskId = asyncService.startBulkImport(5);
    assertThat(taskId).isNotBlank();

    ImportTask task = asyncService.getTask(taskId).orElseThrow();
    assertThat(task.getTotalCount()).isEqualTo(5);
    assertThat(task.getDescription()).contains("5");

    verify(importWorker).runBulkImportAsync(task, 5);
  }

  @Test
  void startSingleImport_shouldReturnTaskIdAndDelegateToWorker() {
    String taskId = asyncService.startSingleImport("Demon Slayer");
    assertThat(taskId).isNotBlank();

    ImportTask task = asyncService.getTask(taskId).orElseThrow();
    assertThat(task.getDescription()).contains("Demon Slayer");

    verify(importWorker).runSingleImportAsync(task, "Demon Slayer");
  }

  @Test
  void getTask_shouldReturnEmptyForUnknownId() {
    Optional<ImportTask> task = asyncService.getTask("non-existent");
    assertThat(task).isEmpty();
  }

  @Test
  void getAllTasks_shouldReturnAllCreatedTasks() {
    asyncService.startSingleImport("One Piece");
    asyncService.startBulkImport(10);
    assertThat(asyncService.getAllTasks()).hasSize(2);
  }

  // Проверка логики самой модели ImportTask
  @Test
  void importTask_progressShouldCalculateCorrectly() {
    ImportTask task = new ImportTask("id", "Test", 4);
    task.setProcessedCount(2);
    assertThat(task.getProgress()).isEqualTo(0.5);
  }

  @Test
  void importTask_progressShouldBeZeroWhenTotalIsZero() {
    ImportTask task = new ImportTask("id", "Test", 0);
    assertThat(task.getProgress()).isEqualTo(0.0);
  }

  @Test
  void importTask_initialStatusShouldBePending() {
    ImportTask task = new ImportTask("id", "Test", 5);
    assertThat(task.getStatus()).isEqualTo(Status.PENDING);
  }
}