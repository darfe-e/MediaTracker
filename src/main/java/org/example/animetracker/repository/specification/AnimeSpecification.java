package org.example.animetracker.repository.specification;

import org.example.animetracker.model.Anime;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class AnimeSpecification {

    private AnimeSpecification() {
    }

    public static Specification<Anime> hasStudio(String studio) {
        return (root, query, cb) ->
            studio == null || studio.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("studio")),
                          "%" + studio.toLowerCase() + "%");
    }

    public static Specification<Anime> hasGenre(String genre) {
        return (root, query, cb) -> {
            if (genre == null || genre.isBlank()) {
              return cb.conjunction();
            }

            try {
                return cb.like(cb.lower(root.get("genre")),
                               "%" + genre.toLowerCase() + "%");
            } catch (Exception e) {
                return cb.conjunction();
            }
        };
    }

    public static Specification<Anime> isAiring(Boolean isAiring) {
        return (root, query, cb) ->
            isAiring == null
                ? cb.conjunction()
                : cb.equal(root.get("isOngoing"), isAiring);
    }

    public static Specification<Anime> minEpisodes(Integer min) {
        return (root, query, cb) ->
            min == null
                ? cb.conjunction()
                : cb.greaterThanOrEqualTo(root.get("duration"), min);
    }

    public static Specification<Anime> buildFilter(
            String studio,
            String genre,
            Integer minEpisodes,
            Boolean isAiring) {

        List<Specification<Anime>> specs = new ArrayList<>();
        if (studio     != null && !studio.isBlank()) {
          specs.add(hasStudio(studio));
        }
        if (genre      != null && !genre.isBlank()) {
          specs.add(hasGenre(genre));
        }
        if (minEpisodes != null) {
          specs.add(minEpisodes(minEpisodes));
        }
        if (isAiring   != null) {
          specs.add(isAiring(isAiring));
        }

        return specs.stream()
                .reduce(Specification.where(null), Specification::and);
    }
}
