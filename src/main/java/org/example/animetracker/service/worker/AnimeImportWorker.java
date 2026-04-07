package org.example.animetracker.service.worker;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.model.ImportTask;
import org.example.animetracker.service.AnimeImportService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnimeImportWorker {

  private final AnimeImportService animeImportService;

  @Async("importExecutor")
  public void runBulkImportAsync(ImportTask task, int limit) {
    task.setStatus(ImportTask.Status.IN_PROGRESS);
    task.setStartedAt(LocalDateTime.now());
    try {
      animeImportService.refreshPopularAnimeWithProgress(limit, task);
      task.setStatus(ImportTask.Status.DONE);
    } catch (Exception e) {
      log.error("[Task {}] Ошибка bulk import: {}", task.getId(), e.getMessage(), e);
      task.setStatus(ImportTask.Status.FAILED);
      task.setError(e.getMessage());
    } finally {
      task.setCompletedAt(LocalDateTime.now());
    }
  }

  @Async("importExecutor")
  public void runSingleImportAsync(ImportTask task, String title) {
    task.setStatus(ImportTask.Status.IN_PROGRESS);
    task.setStartedAt(LocalDateTime.now());
    try {
      animeImportService.importFromApi(title);
      task.setProcessedCount(1);
      task.setStatus(ImportTask.Status.DONE);
    } catch (Exception e) {
      log.error("[Task {}] Ошибка обновления '{}': {}", task.getId(), title, e.getMessage(), e);
      task.setStatus(ImportTask.Status.FAILED);
      task.setError(e.getMessage());
    } finally {
      task.setCompletedAt(LocalDateTime.now());
    }
  }
}