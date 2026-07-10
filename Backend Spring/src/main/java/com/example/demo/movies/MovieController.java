package com.example.demo.movies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/movies")
public class MovieController {

    @Autowired
    private MovieService movieService;

    @GetMapping
    public ResponseEntity<List<Movie>> getAllMovies(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(movieService.allMovies(search, genre, sort));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> getGenres() {
        return ResponseEntity.ok(movieService.allGenres());
    }

    @GetMapping("/trending")
    public ResponseEntity<List<Movie>> getTrending(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(movieService.trending(limit));
    }

    @GetMapping("/{imdbId}")
    public ResponseEntity<Optional<Movie>> getSingleMovie(@PathVariable String imdbId) {
        return new ResponseEntity<>(movieService.findMovieByImdbId(imdbId), HttpStatus.OK);
    }
}
