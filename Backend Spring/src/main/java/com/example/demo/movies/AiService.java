package com.example.demo.movies;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Claude-powered features: review summary + sentiment, natural-language search,
 * and recommendations. Degrades gracefully to a friendly message when no
 * ANTHROPIC_API_KEY is configured, so the rest of the app keeps working.
 */
@Service
public class AiService {

    private final MovieService movieService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AnthropicClient client;   // null when unconfigured
    private final boolean configured;

    public AiService(MovieService movieService,
                     @Value("${ANTHROPIC_API_KEY:}") String apiKey) {
        this.movieService = movieService;
        if (apiKey != null && !apiKey.isBlank()) {
            this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            this.configured = true;
        } else {
            this.client = null;
            this.configured = false;
        }
    }

    private Map<String, Object> notConfigured() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("configured", false);
        m.put("message", "AI features are off. Add an ANTHROPIC_API_KEY to enable them.");
        return m;
    }

    /** Summarize a movie's reviews into a verdict, pros/cons, and a sentiment score. */
    public Map<String, Object> summarizeReviews(String imdbId) {
        if (!configured) return notConfigured();

        Optional<Movie> movieOpt = movieService.findMovieByImdbId(imdbId);
        if (movieOpt.isEmpty()) {
            return error("Movie not found.");
        }
        Movie movie = movieOpt.get();
        List<Review> reviews = movie.getReviewIds();
        if (reviews == null || reviews.isEmpty()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("configured", true);
            m.put("hasData", false);
            m.put("message", "No reviews yet — be the first to review this film.");
            return m;
        }

        StringBuilder reviewText = new StringBuilder();
        for (Review r : reviews) {
            if (r == null || r.getBody() == null) continue;
            reviewText.append("- ");
            if (r.getRating() != null) reviewText.append("(").append(r.getRating()).append("/5) ");
            reviewText.append(r.getBody().trim()).append("\n");
        }

        String prompt = "You are a film critic summarizing audience reviews for the movie \""
                + movie.getTitle() + "\".\n\n"
                + "Reviews:\n" + reviewText + "\n"
                + "Summarize the overall audience reaction. Respond with ONLY a JSON object, no prose, "
                + "in exactly this shape:\n"
                + "{\"verdict\": \"1-2 sentence overall takeaway\", "
                + "\"pros\": [\"short phrase\", \"short phrase\", \"short phrase\"], "
                + "\"cons\": [\"short phrase\"], "
                + "\"sentiment\": 0-100 integer where 100 is universally loved, "
                + "\"sentimentLabel\": \"e.g. Very positive\"}";

        try {
            JsonNode json = mapper.readTree(extractJson(ask(prompt)));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("configured", true);
            m.put("hasData", true);
            m.put("verdict", json.path("verdict").asText(""));
            m.put("pros", toStringList(json.path("pros")));
            m.put("cons", toStringList(json.path("cons")));
            m.put("sentiment", json.path("sentiment").asInt(50));
            m.put("sentimentLabel", json.path("sentimentLabel").asText(""));
            m.put("reviewCount", reviews.size());
            return m;
        } catch (Exception e) {
            return error("Couldn't summarize right now: " + e.getMessage());
        }
    }

    /** Translate a free-text query into a ranked list of matching movies. */
    public Map<String, Object> naturalLanguageSearch(String query) {
        if (!configured) return notConfigured();
        if (query == null || query.isBlank()) return error("Enter something to search for.");

        List<Movie> catalog = movieService.allMovies();
        String catalogJson = catalogJson(catalog);

        String prompt = "A user is searching a movie catalog with this request: \"" + query + "\".\n\n"
                + "Catalog (JSON): " + catalogJson + "\n\n"
                + "Pick the movies that best match the request, most relevant first. "
                + "Respond with ONLY a JSON object: "
                + "{\"imdbIds\": [\"id\", ...], \"explanation\": \"one friendly sentence on why these fit\"}. "
                + "If nothing fits, return an empty imdbIds array.";

        try {
            JsonNode json = mapper.readTree(extractJson(ask(prompt)));
            List<String> ids = toStringList(json.path("imdbIds"));
            Map<String, Movie> byId = catalog.stream()
                    .collect(Collectors.toMap(Movie::getImdbId, mv -> mv, (a, b) -> a));
            List<Movie> results = new ArrayList<>();
            for (String id : ids) {
                Movie mv = byId.get(id);
                if (mv != null) results.add(mv);
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("configured", true);
            m.put("results", results);
            m.put("explanation", json.path("explanation").asText(""));
            return m;
        } catch (Exception e) {
            return error("Search failed: " + e.getMessage());
        }
    }

    /** Recommend similar movies to a given one, each with a one-line reason. */
    public Map<String, Object> recommend(String imdbId) {
        if (!configured) return notConfigured();

        Optional<Movie> seedOpt = movieService.findMovieByImdbId(imdbId);
        if (seedOpt.isEmpty()) return error("Movie not found.");
        Movie seed = seedOpt.get();

        List<Movie> catalog = movieService.allMovies().stream()
                .filter(mv -> !imdbId.equals(mv.getImdbId()))
                .collect(Collectors.toList());

        String prompt = "A viewer just watched \"" + seed.getTitle() + "\" (genres: "
                + (seed.getGenres() == null ? "" : String.join(", ", seed.getGenres())) + ").\n\n"
                + "From this catalog (JSON): " + catalogJson(catalog) + "\n\n"
                + "Recommend up to 4 they'd enjoy next, best first. Respond with ONLY a JSON object: "
                + "{\"recommendations\": [{\"imdbId\": \"id\", \"reason\": \"one short sentence\"}]}.";

        try {
            JsonNode json = mapper.readTree(extractJson(ask(prompt)));
            Map<String, Movie> byId = catalog.stream()
                    .collect(Collectors.toMap(Movie::getImdbId, mv -> mv, (a, b) -> a));
            List<Map<String, Object>> recs = new ArrayList<>();
            for (JsonNode node : json.path("recommendations")) {
                Movie mv = byId.get(node.path("imdbId").asText());
                if (mv == null) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("movie", mv);
                row.put("reason", node.path("reason").asText(""));
                recs.add(row);
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("configured", true);
            m.put("recommendations", recs);
            return m;
        } catch (Exception e) {
            return error("Recommendations failed: " + e.getMessage());
        }
    }

    // ---- helpers ----

    private String ask(String prompt) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-opus-4-8")
                .maxTokens(1500L)
                .addUserMessage(prompt)
                .build();
        Message response = client.messages().create(params);
        StringBuilder sb = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> sb.append(t.text())));
        return sb.toString();
    }

    private String catalogJson(List<Movie> movies) {
        List<Map<String, Object>> rows = movies.stream().map(mv -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("imdbId", mv.getImdbId());
            row.put("title", mv.getTitle());
            row.put("genres", mv.getGenres());
            row.put("releaseDate", mv.getReleaseDate());
            return row;
        }).collect(Collectors.toList());
        try {
            return mapper.writeValueAsString(rows);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** Pull the JSON object out of the model's reply, tolerating stray prose or code fences. */
    private String extractJson(String text) {
        if (text == null) return "{}";
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return "{}";
    }

    private List<String> toStringList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(n -> out.add(n.asText()));
        }
        return out;
    }

    private Map<String, Object> error(String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("configured", true);
        m.put("error", message);
        return m;
    }
}
