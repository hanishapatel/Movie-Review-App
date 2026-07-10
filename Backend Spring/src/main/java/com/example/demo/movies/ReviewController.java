package com.example.demo.movies;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {
    @Autowired
    private ReviewService service;

    @PostMapping
    public ResponseEntity<Review> createReview(@RequestBody Map<String, Object> payload) {
        String body = str(payload.get("reviewBody"));
        String imdbId = str(payload.get("imdbId"));
        String author = str(payload.get("author"));
        Integer rating = toInt(payload.get("rating"));
        boolean spoiler = Boolean.parseBoolean(str(payload.get("spoiler")));
        return new ResponseEntity<>(service.createReview(body, rating, author, spoiler, imdbId), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Review> updateReview(@PathVariable String id, @RequestBody Map<String, Object> payload) {
        String body = str(payload.get("reviewBody"));
        Integer rating = toInt(payload.get("rating"));
        Boolean spoiler = payload.containsKey("spoiler") ? Boolean.parseBoolean(str(payload.get("spoiler"))) : null;
        return ResponseEntity.ok(service.updateReview(id, body, rating, spoiler));
    }

    @PostMapping("/{id}/upvote")
    public ResponseEntity<Review> upvote(@PathVariable String id) {
        return ResponseEntity.ok(service.upvoteReview(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        service.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }

    private static Integer toInt(Object o) {
        if (o == null) return null;
        try {
            return (o instanceof Number) ? ((Number) o).intValue() : Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
