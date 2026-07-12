/*
 * Seed the movie catalog from TMDB into MongoDB.
 *
 * Pulls a deduped mix of Popular + Trending + Top Rated + Now Playing,
 * maps each movie to the app's schema (imdbId, title, releaseDate, poster,
 * backdrops, genres, trailerLink), and upserts into the `movielist` collection.
 * Existing reviews are preserved (reviewIds is only set on insert).
 *
 * Run:
 *   cd scripts && npm install
 *   TMDB_API_KEY=xxx MONGO_URI="mongodb+srv://..." node seed-tmdb.js
 *
 * Optional env: COUNT (default 100), MONGO_DATABASE (default movielist)
 */
const { MongoClient } = require("mongodb");

const API = process.env.TMDB_API_KEY;
const MONGO = process.env.MONGO_URI;
const COUNT = parseInt(process.env.COUNT || "100", 10);
const DB = process.env.MONGO_DATABASE || "movielist";
const COLLECTION = "movielist";
const IMG = "https://image.tmdb.org/t/p";

if (!API || !MONGO) {
  console.error("Missing env. Set TMDB_API_KEY and MONGO_URI.");
  process.exit(1);
}

// Supports both a v3 API key (?api_key=) and a v4 read token (Bearer).
function tmdb(path, params = {}) {
  const url = new URL("https://api.themoviedb.org/3" + path);
  const headers = { accept: "application/json" };
  if (API.startsWith("eyJ")) headers.Authorization = `Bearer ${API}`;
  else url.searchParams.set("api_key", API);
  for (const [k, v] of Object.entries(params)) url.searchParams.set(k, v);
  return fetch(url, { headers }).then(async (r) => {
    if (r.status === 429) {
      await new Promise((res) => setTimeout(res, 1500));
      return tmdb(path, params);
    }
    if (!r.ok) throw new Error(`${r.status} ${url.pathname} — ${await r.text()}`);
    return r.json();
  });
}

// Gather ids from all sources, then interleave (round-robin) + dedupe.
async function collectIds() {
  const buckets = {};
  for (const [key, path, pages] of [
    ["popular", "/movie/popular", [1, 2, 3]],
    ["top_rated", "/movie/top_rated", [1, 2]],
    ["now_playing", "/movie/now_playing", [1, 2]],
  ]) {
    const ids = [];
    for (const page of pages) {
      const d = await tmdb(path, { page });
      ids.push(...d.results.map((r) => r.id));
    }
    buckets[key] = ids;
  }
  buckets.trending = (await tmdb("/trending/movie/week")).results.map((r) => r.id);

  const order = ["popular", "trending", "top_rated", "now_playing"];
  const seen = new Set();
  const merged = [];
  for (let i = 0; merged.length < COUNT && i < 200; i++) {
    for (const k of order) {
      const id = (buckets[k] || [])[i];
      if (id != null && !seen.has(id)) {
        seen.add(id);
        merged.push(id);
      }
    }
  }
  return merged.slice(0, COUNT);
}

async function fetchMovie(id) {
  const m = await tmdb(`/movie/${id}`, {
    append_to_response: "external_ids,videos,images",
  });
  const vids = (m.videos && m.videos.results) || [];
  const trailer =
    vids.find((v) => v.site === "YouTube" && v.type === "Trailer") ||
    vids.find((v) => v.site === "YouTube");
  const backdrops = ((m.images && m.images.backdrops) || [])
    .slice(0, 4)
    .map((b) => `${IMG}/w1280${b.file_path}`);
  return {
    imdbId: (m.external_ids && m.external_ids.imdb_id) || `tmdb${id}`,
    title: m.title,
    releaseDate: m.release_date || "",
    trailerLink: trailer ? `https://www.youtube.com/watch?v=${trailer.key}` : null,
    poster: m.poster_path ? `${IMG}/w500${m.poster_path}` : null,
    backdrops: backdrops.length
      ? backdrops
      : m.backdrop_path
      ? [`${IMG}/w1280${m.backdrop_path}`]
      : [],
    genres: (m.genres || []).map((g) => g.name),
  };
}

// Run promises in small batches to stay well under rate limits.
async function inBatches(items, size, fn) {
  const out = [];
  for (let i = 0; i < items.length; i += size) {
    const batch = items.slice(i, i + size);
    out.push(...(await Promise.all(batch.map(fn))));
    process.stdout.write(`  fetched ${Math.min(i + size, items.length)}/${items.length}\r`);
  }
  return out;
}

(async () => {
  console.log(`Collecting up to ${COUNT} movies from TMDB…`);
  const ids = await collectIds();
  console.log(`Got ${ids.length} unique ids. Fetching details…`);
  const docs = (await inBatches(ids, 8, fetchMovie)).filter((d) => d.title && d.poster);
  console.log(`\nMapped ${docs.length} movies. Writing to MongoDB…`);

  const client = new MongoClient(MONGO);
  await client.connect();
  const coll = client.db(DB).collection(COLLECTION);
  let inserted = 0,
    updated = 0;
  for (const doc of docs) {
    const res = await coll.updateOne(
      { imdbId: doc.imdbId },
      { $set: doc, $setOnInsert: { reviewIds: [] } },
      { upsert: true }
    );
    if (res.upsertedCount) inserted++;
    else updated++;
  }
  await client.close();
  console.log(`Done → ${inserted} inserted, ${updated} updated in ${DB}.${COLLECTION}.`);
})().catch((e) => {
  console.error("\nSeed failed:", e.message);
  process.exit(1);
});
