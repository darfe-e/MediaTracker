package org.example.animetracker.service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.cache.AnimeSearchCache;
import org.example.animetracker.cache.AnimeSearchKey;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.mapper.AnimeMapper;
import org.example.animetracker.model.Anime;
import org.example.animetracker.repository.AnimeRepository;
import org.example.animetracker.repository.specification.AnimeSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Anime not found with id: " + id));
  }

  public List<AnimeDto> findByStudioAndName(String studio, String title) {
    if (studio != null && title != null) {
      List<Anime> list = animeRepository.findByStudioAndTitle(studio, title);
      if (list.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Anime not found with studio: " + studio + " and title: " + title);
      }
      return list.stream().map(AnimeMapper::animeToDto).toList();
    } else if (studio != null) {
      List<Anime> list = animeRepository.findByStudio(studio);
      if (list.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Anime not found with studio: " + studio);
      }
      return list.stream().map(AnimeMapper::animeToDto).toList();
    } else if (title != null) {
      List<Anime> list = animeRepository.findByTitle(title);
      if (list.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Anime not found with title: " + title);
      }
      return list.stream().map(AnimeMapper::animeToDto).toList();
    } else {
      List<Anime> list = animeRepository.findAll();
      if (list.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No anime found in catalogue");
      }
      return list.stream().map(AnimeMapper::animeToDto).toList();
    }
  }

  public List<AnimeDto> findByStudio(String studio) {
    if (studio != null) {
      List<Anime> list = animeRepository.findByStudioContainingIgnoreCase(studio);
      if (list.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "Anime not found with studio: " + studio);
      }
      return list.stream().map(AnimeMapper::animeToDto).toList();
    } else {
      List<Anime> list = animeRepository.findAll();
      if (list.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No anime found in catalogue");
      }
      return list.stream().map(AnimeMapper::animeToDto).toList();
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
      log.debug("Cache hit for key: {}", key);
      return cached;
    }
    log.debug("Cache miss for key: {}", key);

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
      log.debug("Cache hit for key: {}", key);
      return cached;
    }
    log.debug("Cache miss for key: {}", key);

    Page<Anime> animePage = animeRepository
        .findByGenreAndMinSeasonsNative(genre, minSeasons, pageable);
    Page<AnimeDto> result = animePage.map(AnimeMapper::animeToDto);
    searchCache.put(key, result);
    return result;
  }

  public Optional<Anime> findByTitle(String title) {
    return animeRepository.findByTitleIgnoreCase(title);
  }

  public Page<AnimeDto> findByFilters(
      String studio, String genre, Integer minEpisodes,
      Boolean isAiring, Pageable pageable) {

    Specification<Anime> spec =
        AnimeSpecification.buildFilter(studio, genre, minEpisodes, isAiring);

    return animeRepository.findAll(spec, pageable)
        .map(AnimeMapper::animeToDto);
  }

  public List<AnimeDto> searchByTitlePartial(String query) {
    if (query == null || query.isBlank()) {
      return Collections.emptyList();
    }
    return animeRepository
        .findTop10ByTitleContainingIgnoreCaseOrderByPopularityRankDesc(query)
        .stream()
        .map(AnimeMapper::animeToDto)
        .toList();
  }
}