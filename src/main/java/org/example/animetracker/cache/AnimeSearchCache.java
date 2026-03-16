package org.example.animetracker.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.dto.AnimeDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AnimeSearchCache {
  private final Map<AnimeSearchKey, Page<AnimeDto>> cache = new ConcurrentHashMap<>();

  public void put(AnimeSearchKey key, Page<AnimeDto> value) {
    cache.put(key, value);
    log.info("The value was added to cache");
  }

  public Page<AnimeDto> get(AnimeSearchKey key) {
    log.info("The value was gotten from the cache");
    return cache.get(key);
  }

  public void invalidateAll() {
    cache.clear();
    log.info("The cache is clear");
  }
}