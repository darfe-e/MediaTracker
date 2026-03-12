package org.example.animetracker.service;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.cache.AnimeSearchCache;
import org.example.animetracker.cache.AnimeSearchKey;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.mapper.AnimeMapper;
import org.example.animetracker.model.Anime;
import org.example.animetracker.repository.AnimeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@AllArgsConstructor
@Service
public class AnimeService {

  private final AnimeRepository animeRepository;
  private final AnimeSearchCache searchCache;

  @Transactional(readOnly = true)
  public AnimeDetailedDto findByIdWithoutProblem(Long id) {
    return animeRepository.findByIdWithDetails(id)
        .map(AnimeMapper::animeToDetailedDto)
        .map(dto -> {
          dto.setSeasons(dto.getSeasons().stream()
              .filter(s -> !s.getEpisodes().isEmpty())
              .toList());
          return dto;
        })
        .orElse(null);
  }

  public List<AnimeDto> findByStudioAndName(String studio, String title) {
    if (studio != null && title != null) {
      return animeRepository.findByStudioAndTitle(studio, title).stream()
          .map(AnimeMapper::animeToDto)
          .toList();
    } else if (studio != null) {
      return animeRepository.findByStudio(studio).stream()
          .map(AnimeMapper::animeToDto)
          .toList();
    } else if (title != null) {
      return animeRepository.findByTitle(title).stream()
          .map(AnimeMapper::animeToDto)
          .toList();
    } else {
      return animeRepository.findAll().stream()
          .map(AnimeMapper::animeToDto)
          .toList();
    }
  }

  public Page<AnimeDto> getAllSortedByPopularity(Pageable pageable) {
    return animeRepository.findAllSorted(pageable)
        .map(AnimeMapper::animeToDto);
  }

  public Page<AnimeDto> findByGenreAndMinSeasons(String genre, int minSeasons, Pageable pageable) {
    AnimeSearchKey key = new AnimeSearchKey(genre, minSeasons,
        pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

    Page<AnimeDto> cached = searchCache.get(key);
    if (cached != null) {
      return cached;
    }

    Page<Anime> animePage = animeRepository.findByGenreAndMinSeasons(genre, minSeasons, pageable);
    Page<AnimeDto> result = animePage.map(AnimeMapper::animeToDto);
    searchCache.put(key, result);
    return result;
  }

  public Page<AnimeDto> findByGenreAndMinSeasonsNative(String genre,
                                                       int minSeasons, Pageable pageable) {
    AnimeSearchKey key = new AnimeSearchKey(genre, minSeasons,
        pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

    Page<AnimeDto> cached = searchCache.get(key);
    if (cached != null) {
      return cached;
    }

    Page<Anime> animePage = animeRepository
        .findByGenreAndMinSeasonsNative(genre, minSeasons, pageable);
    Page<AnimeDto> result = animePage.map(AnimeMapper::animeToDto);
    searchCache.put(key, result);
    return result;
  }
}

