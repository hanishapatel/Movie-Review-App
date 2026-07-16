package com.example.demo.movies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MovieService {

    @Autowired
    private MovieRepository movieRepository;

    // In-memory cache of the decorated catalog. The catalog rarely changes, so
    // serving browse requests from memory avoids a Mongo round-trip on every
    // click — a big win on low-CPU / free instances. Detail pages
    // (findMovieByImdbId) stay uncached so newly posted reviews show instantly.
    private static final long TTL_MS = 5 * 60 * 1000;
    private volatile List<Movie> cache;
    private volatile long cacheAt;

    private synchronized List<Movie> catalog() {
        long now = System.currentTimeMillis();
        if (cache == null || now - cacheAt > TTL_MS) {
            List<Movie> movies = movieRepository.findAll();
            movies.forEach(this::decorate);
            cache = movies;
            cacheAt = now;
        }
        return cache;
    }

    /** Drop the cache so the next read reflects new data (e.g. after a review). */
    public void invalidate() {
        cache = null;
    }

    /**
     * All movies, optionally filtered by a title search and/or genre, and sorted.
     * sort: "rating" | "reviews" | "title" | "date" (null/other = default order).
     */
    public List<Movie> allMovies(String search, String genre, String sort) {
        List<Movie> movies = catalog();

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            movies = movies.stream()
                    .filter(m -> m.getTitle() != null && m.getTitle().toLowerCase().contains(q))
                    .collect(Collectors.toList());
        }

        if (genre != null && !genre.isBlank() && !genre.equalsIgnoreCase("all")) {
            movies = movies.stream()
                    .filter(m -> m.getGenres() != null &&
                            m.getGenres().stream().anyMatch(g -> g.equalsIgnoreCase(genre)))
                    .collect(Collectors.toList());
        }

        Comparator<Movie> cmp = comparatorFor(sort);
        if (cmp != null) {
            movies = movies.stream().sorted(cmp).collect(Collectors.toList());
        }
        return movies;
    }

    public List<Movie> allMovies() {
        return allMovies(null, null, null);
    }

    public Optional<Movie> findMovieByImdbId(String imdbId) {
        Optional<Movie> movie = movieRepository.findMovieByImdbId(imdbId);
        movie.ifPresent(this::decorate);
        return movie;
    }

    /** Distinct, sorted list of every genre in the catalog. */
    public List<String> allGenres() {
        return catalog().stream()
                .filter(m -> m.getGenres() != null)
                .flatMap(m -> m.getGenres().stream())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /** Most-reviewed, then highest-rated. */
    public List<Movie> trending(int limit) {
        return catalog().stream()
                .sorted(Comparator
                        .comparingInt((Movie m) -> m.getReviewCount() == null ? 0 : m.getReviewCount())
                        .thenComparingDouble(m -> m.getAverageRating() == null ? 0.0 : m.getAverageRating())
                        .reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    private Comparator<Movie> comparatorFor(String sort) {
        if (sort == null) return null;
        switch (sort) {
            case "rating":
                return Comparator.comparingDouble((Movie m) -> m.getAverageRating() == null ? 0.0 : m.getAverageRating()).reversed();
            case "reviews":
                return Comparator.comparingInt((Movie m) -> m.getReviewCount() == null ? 0 : m.getReviewCount()).reversed();
            case "title":
                return Comparator.comparing(m -> m.getTitle() == null ? "" : m.getTitle(), String.CASE_INSENSITIVE_ORDER);
            case "date":
                return Comparator.comparing((Movie m) -> m.getReleaseDate() == null ? "" : m.getReleaseDate()).reversed();
            default:
                return null;
        }
    }

    /** Fill in averageRating + reviewCount from the referenced reviews. */
    private void decorate(Movie m) {
        List<Review> rs = m.getReviewIds();
        if (rs != null && !rs.isEmpty()) {
            double avg = rs.stream()
                    .filter(r -> r != null && r.getRating() != null)
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            m.setAverageRating(Math.round(avg * 10.0) / 10.0);
            m.setReviewCount(rs.size());
        } else {
            m.setAverageRating(0.0);
            m.setReviewCount(0);
        }
    }
}
