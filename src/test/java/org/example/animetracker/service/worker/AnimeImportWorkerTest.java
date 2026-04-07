package org.example.animetracker.service.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.example.animetracker.model.Anime;
import org.example.animetracker.model.ImportTask;
import org.example.animetracker.model.ImportTask.Status;
import org.example.animetracker.service.AnimeImportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnimeImportWorkerTest {

  @Mock
  private AnimeImportService animeImportService;

  @InjectMocks
  private AnimeImportWorker worker;

  @Test
  void runBulkImportAsync_shouldSetDoneOnSuccess() throws Exception {
    ImportTask task = new ImportTask("1", "Test Bulk", 3);

    doAnswer(invocation -> {
      ImportTask t = invocation.getArgument(1);
      t.setProcessedCount(3);
      return null;
    }).when(animeImportService).refreshPopularAnimeWithProgress(anyInt(), any(ImportTask.class));

    worker.runBulkImportAsync(task, 3);

    assertThat(task.getStatus()).isEqualTo(Status.DONE);
    assertThat(task.getProcessedCount()).isEqualTo(3);
    assertThat(task.getStartedAt()).isNotNull();
    assertThat(task.getCompletedAt()).isNotNull();
  }

  @Test
  void runBulkImportAsync_shouldSetFailedOnException() throws Exception {
    ImportTask task = new ImportTask("1", "Test Bulk", 3);

    doThrow(new RuntimeException("API error"))
        .when(animeImportService)
        .refreshPopularAnimeWithProgress(anyInt(), any(ImportTask.class));

    worker.runBulkImportAsync(task, 3);

    assertThat(task.getStatus()).isEqualTo(Status.FAILED);
    assertThat(task.getError()).contains("API error");
    assertThat(task.getCompletedAt()).isNotNull();
  }

  @Test
  void runSingleImportAsync_shouldSetDoneOnSuccess() {
    ImportTask task = new ImportTask("2", "Test Single", 1);
    when(animeImportService.importFromApi("Naruto")).thenReturn(Optional.of(new Anime()));

    worker.runSingleImportAsync(task, "Naruto");

    assertThat(task.getStatus()).isEqualTo(Status.DONE);
    assertThat(task.getProcessedCount()).isEqualTo(1);
    assertThat(task.getCompletedAt()).isNotNull();
  }

  @Test
  void runSingleImportAsync_shouldSetFailedOnException() {
    ImportTask task = new ImportTask("2", "Test Single", 1);
    doThrow(new RuntimeException("Not found")).when(animeImportService).importFromApi("Unknown");

    worker.runSingleImportAsync(task, "Unknown");

    assertThat(task.getStatus()).isEqualTo(Status.FAILED);
    assertThat(task.getError()).contains("Not found");
    assertThat(task.getCompletedAt()).isNotNull();
  }
}