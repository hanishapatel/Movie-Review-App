package com.example.demo.movies;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * On startup, if the movie catalog is empty and TMDB_API_KEY is set, populate it
 * with a deduped mix of popular / trending / top-rated / now-playing movies from
 * TMDB. Runs once in a background thread so it never blocks server startup, and
 * does nothing if the catalog already has movies. No key set → skipped quietly.
 */
@Component
public class MovieSeeder implements ApplicationRunner {

    private static final String IMG = "https://image.tmdb.org/t/p";

    private final MovieRepository repository;
    private final String apiKey;
    private final int count;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public MovieSeeder(MovieRepository repository,
                       @Value("${TMDB_API_KEY:}") String apiKey,
                       @Value("${SEED_COUNT:100}") int count) {
        this.repository = repository;
        this.apiKey = apiKey;
        this.count = count;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("[seed] TMDB_API_KEY not set — skipping auto-seed.");
            return;
        }
        if (repository.count() > 0) {
            System.out.println("[seed] Catalog already populated — skipping auto-seed.");
            return;
        }
        // Don't block startup / health checks.
        Thread t = new Thread(this::seed, "movie-seeder");
        t.setDaemon(true);
        t.start();
    }

    private void seed() {
        try {
            System.out.println("[seed] Empty catalog — fetching up to " + count + " movies from TMDB…");
            List<Integer> ids = collectIds();
            int saved = 0;
            for (Integer id : ids) {
                try {
                    Movie m = fetchMovie(id);
                    if (m.getTitle() != null && m.getPoster() != null) {
                        repository.save(m);
                        saved++;
                    }
                } catch (Exception ignore) {
                    // skip a movie that fails to fetch/map
                }
            }
            System.out.println("[seed] Done — saved " + saved + " movies.");
        } catch (Exception e) {
            System.out.println("[seed] Auto-seed failed: " + e.getMessage());
        }
    }

    // Collect ids from all sources and interleave (round-robin), deduped.
    private List<Integer> collectIds() throws Exception {
        Map<String, List<Integer>> buckets = new LinkedHashMap<>();
        buckets.put("popular", listIds("/movie/popular", 1, 2, 3));
        buckets.put("trending", resultIds(tmdb("/trending/movie/week", Map.of())));
        buckets.put("top_rated", listIds("/movie/top_rated", 1, 2));
        buckets.put("now_playing", listIds("/movie/now_playing", 1, 2));

        String[] order = {"popular", "trending", "top_rated", "now_playing"};
        Set<Integer> seen = new LinkedHashSet<>();
        for (int i = 0; seen.size() < count && i < 200; i++) {
            for (String k : order) {
                List<Integer> arr = buckets.get(k);
                if (arr != null && i < arr.size()) seen.add(arr.get(i));
            }
        }
        List<Integer> merged = new ArrayList<>(seen);
        return merged.subList(0, Math.min(count, merged.size()));
    }

    private List<Integer> listIds(String path, int... pages) throws Exception {
        List<Integer> ids = new ArrayList<>();
        for (int p : pages) {
            ids.addAll(resultIds(tmdb(path, Map.of("page", String.valueOf(p)))));
        }
        return ids;
    }

    private List<Integer> resultIds(JsonNode node) {
        List<Integer> ids = new ArrayList<>();
        for (JsonNode r : node.path("results")) ids.add(r.path("id").asInt());
        return ids;
    }

    private Movie fetchMovie(int id) throws Exception {
        JsonNode m = tmdb("/movie/" + id, Map.of("append_to_response", "external_ids,videos,images"));

        String imdbId = m.path("external_ids").path("imdb_id").asText("");
        if (imdbId.isBlank()) imdbId = "tmdb" + id;

        // trailer
        String trailer = null;
        for (JsonNode v : m.path("videos").path("results")) {
            if ("YouTube".equals(v.path("site").asText())) {
                trailer = "https://www.youtube.com/watch?v=" + v.path("key").asText();
                if ("Trailer".equals(v.path("type").asText())) break; // prefer a Trailer
            }
        }

        // backdrops
        List<String> backdrops = new ArrayList<>();
        for (JsonNode b : m.path("images").path("backdrops")) {
            backdrops.add(IMG + "/w1280" + b.path("file_path").asText());
            if (backdrops.size() >= 4) break;
        }
        if (backdrops.isEmpty() && !m.path("backdrop_path").isNull() && m.hasNonNull("backdrop_path")) {
            backdrops.add(IMG + "/w1280" + m.path("backdrop_path").asText());
        }

        // genres
        List<String> genres = new ArrayList<>();
        for (JsonNode g : m.path("genres")) genres.add(g.path("name").asText());

        String poster = m.hasNonNull("poster_path") ? IMG + "/w500" + m.path("poster_path").asText() : null;

        Movie movie = new Movie(
                imdbId,
                m.path("title").asText(),
                m.path("release_date").asText(""),
                trailer,
                poster,
                backdrops,
                genres
        );
        return movie;
    }

    // Supports both a v3 API key (?api_key=) and a v4 read token (Bearer).
    private JsonNode tmdb(String path, Map<String, String> params) throws Exception {
        Map<String, String> p = new LinkedHashMap<>(params);
        HttpRequest.Builder rb = HttpRequest.newBuilder().header("accept", "application/json");
        if (apiKey.startsWith("eyJ")) rb.header("Authorization", "Bearer " + apiKey);
        else p.put("api_key", apiKey);

        StringBuilder url = new StringBuilder("https://api.themoviedb.org/3").append(path).append("?");
        boolean first = true;
        for (Map.Entry<String, String> e : p.entrySet()) {
            if (!first) url.append("&");
            url.append(e.getKey()).append("=").append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            first = false;
        }

        HttpResponse<String> res = http.send(
                rb.uri(URI.create(url.toString())).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(res.body());
    }
}
