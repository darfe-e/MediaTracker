package org.example.animetracker.service;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.repository.AnimeRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogCrawlerService {

  private final AnimeImportService animeImportService;
  private final AnimeRepository    animeRepository;

  private final AtomicBoolean crawling    = new AtomicBoolean(false);
  private final AtomicInteger currentPage = new AtomicInteger(0);

  private static final int PAGE_SIZE      = 50;    // аниме за один запрос
  private static final int MAX_PAGES      = 20;    // 20 страниц × 50 = 1000 аниме
  private static final int DELAY_BETWEEN_PAGES_MS = 30_000; // 30 сек между страницами

  @Async("catalogCrawlerExecutor")
  @Scheduled(fixedDelay = 30 * 60 * 1000, initialDelay = 5 * 60 * 1000) // старт через 5 мин после запуска
  public void crawlNextPage() {
    long count = animeRepository.count();
    if (count >= (long) PAGE_SIZE * MAX_PAGES) {
      log.debug("Каталог уже полон ({} аниме), пропускаем подгрузку", count);
      return;
    }
    if (!crawling.compareAndSet(false, true)) {
      log.debug("Подгрузка уже идёт, пропускаем");
      return;
    }
    try {
      doLoadNextPage();
    } finally {
      crawling.set(false);
    }
  }

  private void doLoadNextPage() {
    int page = currentPage.get();
    if (page >= MAX_PAGES) {
      log.info("Каталог загружен полностью ({} страниц)", MAX_PAGES);
      currentPage.set(0); // сбрасываем для повторной проверки
      return;
    }

    log.info("Фоновая подгрузка: страница {}/{} (по {} аниме)", page + 1, MAX_PAGES, PAGE_SIZE);
    try {
      animeImportService.importPageFromAnilist(page, PAGE_SIZE);
      currentPage.incrementAndGet();

      Thread.sleep(DELAY_BETWEEN_PAGES_MS);
    } catch (InterruptedException ie) {
      Thread.currentThread().interrupt();
      log.warn("Фоновая подгрузка прервана");
    } catch (Exception e) {
      log.error("Ошибка фоновой подгрузки страницы {}: {}", page, e.getMessage());
    }
  }

  public CrawlStatus getStatus() {
    return new CrawlStatus(crawling.get(), currentPage.get(), animeRepository.count());
  }

  public record CrawlStatus(boolean active, int currentPage, long totalInDb) {}
}
