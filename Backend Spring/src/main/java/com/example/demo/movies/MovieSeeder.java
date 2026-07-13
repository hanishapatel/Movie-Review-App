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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * On first startup, if the catalog is empty, auto-populate it from a movie API.
 *
 * Two sources are supported (set ONE key):
 *   - OMDB_API_KEY  → OMDb. Posters are Amazon-hosted (work on ISPs that block
 *                     TMDB, e.g. in India). Seeds from a curated title list.
 *   - TMDB_API_KEY  → TMDB. Popular/trending/top-rated mix with trailers/backdrops.
 *
 * Runs once, in a background thread (never blocks startup). Skips if the catalog
 * is non-empty or no key is set.
 */
@Component
public class MovieSeeder implements ApplicationRunner {

    private static final String TMDB_IMG = "https://image.tmdb.org/t/p";

    private final MovieRepository repository;
    private final String omdbKey;
    private final String tmdbKey;
    private final int count;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    public MovieSeeder(MovieRepository repository,
                       @Value("${OMDB_API_KEY:}") String omdbKey,
                       @Value("${TMDB_API_KEY:}") String tmdbKey,
                       @Value("${SEED_COUNT:100}") int count) {
        this.repository = repository;
        this.omdbKey = omdbKey;
        this.tmdbKey = tmdbKey;
        this.count = count;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean hasKey = notBlank(omdbKey) || notBlank(tmdbKey);
        if (!hasKey) {
            System.out.println("[seed] No OMDB_API_KEY or TMDB_API_KEY set — skipping auto-seed.");
            return;
        }
        if (repository.count() > 0) {
            System.out.println("[seed] Catalog already populated — skipping auto-seed.");
            return;
        }
        Thread t = new Thread(this::seed, "movie-seeder");
        t.setDaemon(true);
        t.start();
    }

    private void seed() {
        try {
            List<Movie> movies = notBlank(omdbKey) ? fromOmdb() : fromTmdb();
            int saved = 0;
            for (Movie m : movies) {
                if (m != null && m.getTitle() != null && m.getPoster() != null) {
                    try {
                        repository.save(m);
                        saved++;
                    } catch (Exception ignore) { /* skip dup/bad doc */ }
                }
            }
            System.out.println("[seed] Done — saved " + saved + " movies.");
        } catch (Exception e) {
            System.out.println("[seed] Auto-seed failed: " + e.getMessage());
        }
    }

    // ---------- OMDb (India-friendly posters) ----------

    private static final List<String> TITLES = Arrays.asList(
            "The Shawshank Redemption", "The Dark Knight", "Inception", "Interstellar", "The Matrix",
            "Pulp Fiction", "Fight Club", "Forrest Gump", "The Godfather", "Gladiator",
            "Titanic", "Avatar", "The Avengers", "Avengers: Endgame", "Iron Man",
            "Spider-Man: No Way Home", "Joker", "Parasite", "Whiplash", "La La Land",
            "The Wolf of Wall Street", "Django Unchained", "Dunkirk", "Tenet", "Oppenheimer",
            "Barbie", "Everything Everywhere All at Once", "Dune", "Blade Runner 2049", "Mad Max: Fury Road",
            "John Wick", "Deadpool", "Guardians of the Galaxy", "Black Panther", "Doctor Strange",
            "Frozen", "Toy Story", "Finding Nemo", "Coco", "Up",
            "Inside Out", "The Lion King", "Shrek", "Spirited Away", "Your Name",
            "The Batman", "Top Gun: Maverick", "No Country for Old Men", "The Prestige", "Memento",
            "Gone Girl", "Se7en", "The Departed", "Goodfellas", "Schindler's List",
            "Saving Private Ryan", "The Silence of the Lambs", "Jurassic Park", "Back to the Future", "Casino"
    );

    private List<Movie> fromOmdb() throws Exception {
        System.out.println("[seed] Empty catalog — fetching movies from OMDb…");
        List<Movie> out = new ArrayList<>();
        for (String title : TITLES) {
            if (out.size() >= count) break;
            try {
                JsonNode m = omdb(title);
                if (!"True".equalsIgnoreCase(m.path("Response").asText())) continue;
                String poster = m.path("Poster").asText("");
                if (poster.isBlank() || "N/A".equalsIgnoreCase(poster)) continue;
                List<String> genres = new ArrayList<>();
                for (String g : m.path("Genre").asText("").split(",")) {
                    if (!g.isBlank()) genres.add(g.trim());
                }
                out.add(new Movie(
                        m.path("imdbID").asText(),
                        m.path("Title").asText(),
                        m.path("Year").asText(""),   // year string; the UI shows the first 4 chars
                        null,                          // OMDb has no trailer
                        poster,
                        new ArrayList<>(),             // OMDb has no backdrops
                        genres
                ));
            } catch (Exception ignore) { /* skip a title that fails */ }
        }
        return out;
    }

    private JsonNode omdb(String title) throws Exception {
        String url = "https://www.omdbapi.com/?apikey=" + URLEncoder.encode(omdbKey, StandardCharsets.UTF_8)
                + "&type=movie&t=" + URLEncoder.encode(title, StandardCharsets.UTF_8);
        HttpResponse<String> res = http.send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        return mapper.readTree(res.body());
    }

    // ---------- TMDB (fallback if TMDB isn't blocked for you) ----------

    private List<Movie> fromTmdb() throws Exception {
        System.out.println("[seed] Empty catalog — fetching movies from TMDB…");
        List<Integer> ids = tmdbIds();
        List<Movie> out = new ArrayList<>();
        for (Integer id : ids) {
            try {
                out.add(fetchTmdbMovie(id));
            } catch (Exception ignore) { /* skip */ }
        }
        return out;
    }

    private List<Integer> tmdbIds() throws Exception {
        Map<String, List<Integer>> buckets = new LinkedHashMap<>();
        buckets.put("popular", tmdbListIds("/movie/popular", 1, 2, 3));
        buckets.put("trending", tmdbResultIds(tmdb("/trending/movie/week", Map.of())));
        buckets.put("top_rated", tmdbListIds("/movie/top_rated", 1, 2));
        buckets.put("now_playing", tmdbListIds("/movie/now_playing", 1, 2));

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

    private List<Integer> tmdbListIds(String path, int... pages) throws Exception {
        List<Integer> ids = new ArrayList<>();
        for (int p : pages) ids.addAll(tmdbResultIds(tmdb(path, Map.of("page", String.valueOf(p)))));
        return ids;
    }

    private List<Integer> tmdbResultIds(JsonNode node) {
        List<Integer> ids = new ArrayList<>();
        for (JsonNode r : node.path("results")) ids.add(r.path("id").asInt());
        return ids;
    }

    private Movie fetchTmdbMovie(int id) throws Exception {
        JsonNode m = tmdb("/movie/" + id, Map.of("append_to_response", "external_ids,videos,images"));
        String imdbId = m.path("external_ids").path("imdb_id").asText("");
        if (imdbId.isBlank()) imdbId = "tmdb" + id;

        String trailer = null;
        for (JsonNode v : m.path("videos").path("results")) {
            if ("YouTube".equals(v.path("site").asText())) {
                trailer = "https://www.youtube.com/watch?v=" + v.path("key").asText();
                if ("Trailer".equals(v.path("type").asText())) break;
            }
        }
        List<String> backdrops = new ArrayList<>();
        for (JsonNode b : m.path("images").path("backdrops")) {
            backdrops.add(TMDB_IMG + "/w1280" + b.path("file_path").asText());
            if (backdrops.size() >= 4) break;
        }
        List<String> genres = new ArrayList<>();
        for (JsonNode g : m.path("genres")) genres.add(g.path("name").asText());
        String poster = m.hasNonNull("poster_path") ? TMDB_IMG + "/w500" + m.path("poster_path").asText() : null;

        return new Movie(imdbId, m.path("title").asText(), m.path("release_date").asText(""),
                trailer, poster, backdrops, genres);
    }

    private JsonNode tmdb(String path, Map<String, String> params) throws Exception {
        Map<String, String> p = new LinkedHashMap<>(params);
        HttpRequest.Builder rb = HttpRequest.newBuilder().header("accept", "application/json");
        if (tmdbKey.startsWith("eyJ")) rb.header("Authorization", "Bearer " + tmdbKey);
        else p.put("api_key", tmdbKey);
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

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
