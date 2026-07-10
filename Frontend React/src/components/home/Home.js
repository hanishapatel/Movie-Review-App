import React, { useState } from "react";
import MovieCard from "../movieCard/MovieCard";

const SORTS = [
  { value: "rating", label: "Top rated" },
  { value: "reviews", label: "Most reviewed" },
  { value: "title", label: "A–Z" },
  { value: "date", label: "Newest" },
];

const Home = ({
  movies = [],
  trending = [],
  genres = [],
  genre,
  setGenre,
  sort,
  setSort,
  ai,
  aiSearch,
  clearAi,
}) => {
  const [query, setQuery] = useState("");

  const submitAi = (e) => {
    e.preventDefault();
    if (query.trim()) aiSearch(query.trim());
  };

  const chips = ["All", ...genres];

  return (
    <div className="browse">
      {/* AI natural-language search */}
      <form className="ai-search" onSubmit={submitAi}>
        <span className="ai-tag">✦ Ask AI</span>
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Try “feel-good sci-fi with a strong lead”…"
        />
        <button type="submit">Search</button>
        {ai && (
          <button
            type="button"
            className="ai-clear"
            onClick={() => {
              setQuery("");
              clearAi();
            }}
          >
            Clear
          </button>
        )}
      </form>

      {/* AI results view */}
      {ai ? (
        <section className="content">
          <div className="sec-title">
            <span className="bar" />✦ AI results for “{ai.query}”
          </div>
          {ai.loading && <p className="muted">Thinking…</p>}
          {!ai.loading && ai.error && <p className="error-text">{ai.error}</p>}
          {!ai.loading && ai.configured === false && (
            <p className="muted">{ai.message}</p>
          )}
          {!ai.loading && ai.explanation && (
            <p className="ai-explain">{ai.explanation}</p>
          )}
          {!ai.loading && ai.results && ai.results.length === 0 && !ai.error && (
            <p className="muted">No matches — try describing it differently.</p>
          )}
          {!ai.loading && ai.results && ai.results.length > 0 && (
            <div className="grid">
              {ai.results.map((m) => (
                <MovieCard key={m.imdbId} movie={m} />
              ))}
            </div>
          )}
        </section>
      ) : (
        <>
          {/* Filters + sort */}
          <div className="filters">
            {chips.map((c) => (
              <button
                key={c}
                className={`chip ${genre === c ? "on" : ""}`}
                onClick={() => setGenre(c)}
              >
                {c}
              </button>
            ))}
            <label className="sort">
              Sort:
              <select value={sort} onChange={(e) => setSort(e.target.value)}>
                {SORTS.map((s) => (
                  <option key={s.value} value={s.value}>
                    {s.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {/* Trending */}
          {trending && trending.length > 0 && (
            <section className="content">
              <div className="sec-title">
                <span className="bar" />🔥 Trending now
              </div>
              <div className="row">
                {trending.map((m, i) => (
                  <div className="row-item" key={m.imdbId}>
                    <MovieCard movie={m} rank={i + 1} />
                  </div>
                ))}
              </div>
            </section>
          )}

          {/* All movies */}
          <section className="content">
            <div className="sec-title">
              <span className="bar" />
              {genre === "All" ? "All movies" : genre}
            </div>
            {movies.length === 0 ? (
              <p className="muted">
                No movies to show. Is the backend running on :8080?
              </p>
            ) : (
              <div className="grid">
                {movies.map((m) => (
                  <MovieCard key={m.imdbId} movie={m} />
                ))}
              </div>
            )}
          </section>
        </>
      )}
    </div>
  );
};

export default Home;
