package org.example.animetracker.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.model.ImportTask;
import org.example.animetracker.service.worker.AnimeImportWorker;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncAnimeImportService {

  private final AnimeImportWorker importWorker;
  private final Map<String, ImportTask> tasks = new ConcurrentHashMap<>();

  public String startBulkImport(int limit) {
    String taskId = UUID.randomUUID().toString();
    ImportTask task = new ImportTask(taskId, "Импорт топ-" + limit + " аниме", limit);
    tasks.put(taskId, task);

    importWorker.runBulkImportAsync(task, limit);

    log.info("Задача импорта создана: {} (limit={})", taskId, limit);
    return taskId;
  }

  public String startSingleImport(String title) {
    String taskId = UUID.randomUUID().toString();
    ImportTask task = new ImportTask(taskId, "Обновление: " + title, 1);
    tasks.put(taskId, task);

    importWorker.runSingleImportAsync(task, title);

    log.info("Фоновое обновление запущено: {} (title='{}')", taskId, title);
    return taskId;
  }

  public Optional<ImportTask> getTask(String taskId) {
    return Optional.ofNullable(tasks.get(taskId));
  }

  public Collection<ImportTask> getAllTasks() {
    return tasks.values();
  }
}