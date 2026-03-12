package org.example.animetracker.cache;

import java.util.Objects;
import org.springframework.data.domain.Sort;

public class AnimeSearchKey {
  private final String genre;
  private final int minSeasons;
  private final int page;
  private final int size;
  private final String sortString;

  public AnimeSearchKey(String genre, int minSeasons, int page, int size, Sort sort) {
    this.genre = genre;
    this.minSeasons = minSeasons;
    this.page = page;
    this.size = size;
    this.sortString = sort.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AnimeSearchKey that = (AnimeSearchKey) o;
    return minSeasons == that.minSeasons
        && page == that.page
        && size == that.size
        && Objects.equals(genre, that.genre)
        && Objects.equals(sortString, that.sortString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(genre, minSeasons, page, size, sortString);
  }
}