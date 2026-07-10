package com.example.demo.movies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    @Autowired
    private AiService aiService;

    /** AI summary + sentiment for a movie's reviews. */
    @PostMapping("/summary")
    public ResponseEntity<Map<String, Object>> summary(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(aiService.summarizeReviews(payload.get("imdbId")));
    }

    /** Natural-language movie search ("feel-good 90s sci-fi"). */
    @PostMapping("/search")
    public ResponseEntity<Map<String, Object>> search(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(aiService.naturalLanguageSearch(payload.get("query")));
    }

    /** "Because you watched X" recommendations. */
    @PostMapping("/recommend")
    public ResponseEntity<Map<String, Object>> recommend(@RequestBody Map<String, String> payload) {
        return ResponseEntity.ok(aiService.recommend(payload.get("imdbId")));
    }
}
