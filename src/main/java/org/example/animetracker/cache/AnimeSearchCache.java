package org.example.animetracker.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.example.animetracker.dto.AnimeDto;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
public class AnimeSearchCache {
  private final Map<AnimeSearchKey, Page<AnimeDto>> cache = new ConcurrentHashMap<>();

  public void put(AnimeSearchKey key, Page<AnimeDto> value) {
    cache.put(key, value);
  }

  public Page<AnimeDto> get(AnimeSearchKey key) {
    return cache.get(key);
  }

  public void invalidateAll() {
    cache.clear();
  }
}