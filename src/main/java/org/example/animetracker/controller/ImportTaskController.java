package org.example.animetracker.controller;

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.example.animetracker.model.ImportTask;
import org.example.animetracker.service.AsyncAnimeImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/import")
@RequiredArgsConstructor
public class ImportTaskController {

  private static final String ERROR_KEY = "error";
  private final AsyncAnimeImportService asyncImportService;

  @PostMapping("/bulk")
  public ResponseEntity<Map<String, String>> startBulkImport(
      @RequestParam(defaultValue = "5") int limit) {

    if (limit < 1 || limit > 50) {
      return ResponseEntity.badRequest()
          .body(Map.of(ERROR_KEY, "limit должен быть от 1 до 50"));
    }

    String taskId = asyncImportService.startBulkImport(limit);
    return ResponseEntity.accepted().body(Map.of(
        "taskId", taskId,
        "message", "Задача импорта запущена. Следи за статусом через GET /import/tasks/" + taskId
    ));
  }

  @PostMapping("/single")
  public ResponseEntity<Map<String, String>> startSingleImport(
      @RequestParam String title) {

    if (title == null || title.isBlank()) {
      return ResponseEntity.badRequest()
          .body(Map.of(ERROR_KEY, "title не может быть пустым"));
    }

    String taskId = asyncImportService.startSingleImport(title);
    return ResponseEntity.accepted().body(Map.of(
        "taskId", taskId,
        "message", "Обновление '" + title + "' запущено в фоне",
        "statusUrl", "/import/tasks/" + taskId
    ));
  }

  @GetMapping("/tasks/{taskId}")
  public ResponseEntity
      <Map<String, Object>> getTaskStatus(@PathVariable String taskId) {
    return asyncImportService.getTask(taskId)
        .map(task -> ResponseEntity.ok(toResponse(task)))
        .orElse(ResponseEntity.notFound().build());
  }

  @GetMapping("/tasks")
  public ResponseEntity<Collection<Map<String, Object>>> getAllTasks() {
    Collection<Map<String, Object>> response = asyncImportService.getAllTasks()
        .stream()
        .map(this::toResponse)
        .toList();
    return ResponseEntity.ok(response);
  }

  private Map<String, Object> toResponse(ImportTask task) {
    return Map.of(
        "id",             task.getId(),
        "description",    task.getDescription(),
        "status",         task.getStatus().name(),
        "processedCount", task.getProcessedCount(),
        "totalCount",     task.getTotalCount(),
        "progress",       String.format("%.0f%%", task.getProgress() * 100),
        "createdAt",      task.getCreatedAt().toString(),
        "startedAt",      task.getStartedAt() != null ? task.getStartedAt().toString() : "",
        "completedAt",    task.getCompletedAt() != null ? task.getCompletedAt().toString() : "",
        ERROR_KEY,          task.getError() != null ? task.getError() : ""
    );
  }
}