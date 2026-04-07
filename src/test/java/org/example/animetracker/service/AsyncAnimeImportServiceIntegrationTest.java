package org.example.animetracker.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.example.animetracker.config.AsyncConfig;
import org.example.animetracker.model.ImportTask;
import org.example.animetracker.model.ImportTask.Status;
import org.example.animetracker.service.worker.AnimeImportWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// ДОБАВЛЕН AnimeImportWorker.class в контекст!
@SpringBootTest(classes = {AsyncAnimeImportService.class, AnimeImportWorker.class, AsyncConfig.class})
@EnableAsync
class AsyncAnimeImportServiceIntegrationTest {

  @Autowired
  private AsyncAnimeImportService asyncService;

  @MockitoBean
  private AnimeImportService animeImportService;

  @Test
  void bulkImport_shouldReturnImmediatelyAndTaskShouldCompleteInBackground() throws Exception {
    CountDownLatch taskStarted   = new CountDownLatch(1);
    CountDownLatch letTaskFinish = new CountDownLatch(1);

    doAnswer(invocation -> {
      ImportTask task = invocation.getArgument(1);
      taskStarted.countDown();
      letTaskFinish.await();
      task.setProcessedCount(3);
      return null;
    }).when(animeImportService).refreshPopularAnimeWithProgress(anyInt(), any(ImportTask.class));

    String taskId = asyncService.startBulkImport(3);

    boolean taskActuallyStarted = taskStarted.await(5, TimeUnit.SECONDS);
    assertThat(taskActuallyStarted).isTrue();

    ImportTask task = asyncService.getTask(taskId).orElseThrow();
    assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);

    letTaskFinish.countDown();

    await().atMost(5, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS).untilAsserted(() -> {
      assertThat(task.getStatus()).isEqualTo(Status.DONE);
      assertThat(task.getProcessedCount()).isEqualTo(3);
      assertThat(task.getCompletedAt()).isNotNull();
    });
  }

  @Test
  void bulkImport_shouldSetStatusFailedWhenServiceThrows() throws Exception {
    doThrow(new RuntimeException("API недоступен"))
        .when(animeImportService)
        .refreshPopularAnimeWithProgress(anyInt(), any(ImportTask.class));

    String taskId = asyncService.startBulkImport(2);
    ImportTask task = asyncService.getTask(taskId).orElseThrow();

    await().atMost(10, TimeUnit.SECONDS).pollInterval(100, TimeUnit.MILLISECONDS).untilAsserted(() -> {
      assertThat(task.getStatus()).isEqualTo(Status.FAILED);
      assertThat(task.getError()).contains("API недоступен");
    });
  }

  @Test
  void singleImport_shouldStartInBackgroundAndComplete() throws InterruptedException {
    CountDownLatch taskStarted   = new CountDownLatch(1);
    CountDownLatch letTaskFinish = new CountDownLatch(1);

    doAnswer(invocation -> {
      taskStarted.countDown();
      letTaskFinish.await();
      return Optional.empty();
    }).when(animeImportService).importFromApi(anyString());

    String taskId = asyncService.startSingleImport("Naruto");

    boolean started = taskStarted.await(5, TimeUnit.SECONDS);
    assertThat(started).isTrue();

    ImportTask task = asyncService.getTask(taskId).orElseThrow();
    assertThat(task.getStatus()).isEqualTo(Status.IN_PROGRESS);

    letTaskFinish.countDown();

    await().atMost(5, TimeUnit.SECONDS).pollInterval(50, TimeUnit.MILLISECONDS).untilAsserted(() ->
        assertThat(task.getStatus()).isIn(Status.DONE, Status.FAILED));
  }

  @Test
  void multipleTasks_shouldQueueAndExecuteSequentially() throws Exception {
    doAnswer(invocation -> {
      ImportTask task = invocation.getArgument(1);
      task.setProcessedCount(1);
      return null;
    }).when(animeImportService).refreshPopularAnimeWithProgress(anyInt(), any(ImportTask.class));

    String taskId1 = asyncService.startBulkImport(1);
    String taskId2 = asyncService.startBulkImport(1);
    String taskId3 = asyncService.startBulkImport(1);

    await().atMost(15, TimeUnit.SECONDS).pollInterval(200, TimeUnit.MILLISECONDS).untilAsserted(() -> {
      assertThat(asyncService.getTask(taskId1).orElseThrow().getStatus()).isEqualTo(Status.DONE);
      assertThat(asyncService.getTask(taskId2).orElseThrow().getStatus()).isEqualTo(Status.DONE);
      assertThat(asyncService.getTask(taskId3).orElseThrow().getStatus()).isEqualTo(Status.DONE);
    });
  }
}