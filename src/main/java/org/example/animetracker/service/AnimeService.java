package org.example.animetracker.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.example.animetracker.dto.AnimeDetailedDto;
import org.example.animetracker.dto.AnimeDto;
import org.example.animetracker.mapper.AnimeMapper;
import org.example.animetracker.model.Anime;
import org.example.animetracker.repository.AnimeRepository;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class AnimeService {

  private final AnimeRepository animeRepository;

  public AnimeDetailedDto findById(Long id) {
    Anime anime = animeRepository.findById(id);
    if (anime == null) {
      return null;
    }
    return AnimeMapper.animeToDetailedDto(anime);
  }

  public List<AnimeDto> findByStudioAndName(String studio, String title) {
    return animeRepository.findAll().stream()
        .filter(a -> studio == null || studio.equals(a.getStudio()))
        .filter(a -> title == null || title.equals(a.getTitle()))
        .map(AnimeMapper::animeToDto)
        .collect(Collectors.toList());
  }

  public List<AnimeDto> getAllSortedByPopularity() {
    return animeRepository.findAll().stream()
        .sorted(Comparator.comparingInt(Anime::getPopularityRank).reversed())
        .map(AnimeMapper::animeToDto)
        .collect(Collectors.toList());
  }
}