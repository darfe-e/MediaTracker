package org.example.animetracker.controller;

import lombok.RequiredArgsConstructor;
import org.example.animetracker.service.CatalogCrawlerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

  private final CatalogCrawlerService crawlerService;

  @GetMapping("/crawler-status")
  public ResponseEntity<CatalogCrawlerService.CrawlStatus> getStatus() {
    return ResponseEntity.ok(crawlerService.getStatus());
  }
}
