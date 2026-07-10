import React, { useEffect, useState } from "react";
import api from "../../api/axiosConfig";
import { useParams, useNavigate } from "react-router-dom";
import ReviewForm from "../reviewForm/ReviewForm";
import Stars from "../stars/Stars";

const initials = (name) =>
  (name || "A")
    .split(" ")
    .map((w) => w[0])
    .slice(0, 2)
    .join("")
    .toUpperCase();

// One review row, with its own spoiler-reveal state.
const ReviewItem = ({ r, onUpvote, onDelete }) => {
  const [revealed, setRevealed] = useState(false);
  const hidden = r.spoiler && !revealed;
  return (
    <div className="rev">
      <div className="rev-top">
        <span className="avatar">{initials(r.author)}</span>
        <div>
          <div className="who">{r.author || "Anonymous"}</div>
          <Stars value={r.rating || 0} size={12} />
        </div>
        {r.spoiler && <span className="spoiler-tag">⚠ Spoiler</span>}
        {r.created && (
          <span className="when">{String(r.created).slice(0, 10)}</span>
        )}
      </div>
      <div className={`rev-body ${hidden ? "blurred" : ""}`}>{r.body}</div>
      {hidden ? (
        <button className="reveal" onClick={() => setRevealed(true)}>
          🔒 Contains spoilers · tap to reveal
        </button>
      ) : (
        <div className="rev-foot">
          <button className="up" onClick={() => onUpvote(r.id)}>
            ▲ Helpful <b>{r.upvotes || 0}</b>
          </button>
          <button className="del" onClick={() => onDelete(r.id)}>
            Delete
          </button>
        </div>
      )}
    </div>
  );
};

const Reviews = ({ getMovieData, movie, reviews = [] }) => {
  const { movieId } = useParams();
  const navigate = useNavigate();
  const [summary, setSummary] = useState(null); // {loading,...}
  const [recs, setRecs] = useState(null);

  useEffect(() => {
    getMovieData(movieId);
    setSummary(null);
    setRecs(null);
  }, [movieId]);

  const refresh = () => getMovieData(movieId);

  const addReview = async ({ rating, author, body, spoiler }) => {
    try {
      await api.post("/api/v1/reviews", {
        reviewBody: body,
        imdbId: movieId,
        rating,
        author,
        spoiler,
      });
      refresh();
    } catch (err) {
      console.error(err);
    }
  };

  const upvote = async (id) => {
    try {
      await api.post(`/api/v1/reviews/${id}/upvote`);
      refresh();
    } catch (err) {
      console.error(err);
    }
  };

  const del = async (id) => {
    try {
      await api.delete(`/api/v1/reviews/${id}`);
      refresh();
    } catch (err) {
      console.error(err);
    }
  };

  const loadSummary = async () => {
    setSummary({ loading: true });
    try {
      const res = await api.post("/api/v1/ai/summary", { imdbId: movieId });
      setSummary({ loading: false, ...res.data });
    } catch (e) {
      setSummary({ loading: false, error: "Couldn't reach the AI service." });
    }
  };

  const loadRecs = async () => {
    setRecs({ loading: true });
    try {
      const res = await api.post("/api/v1/ai/recommend", { imdbId: movieId });
      setRecs({ loading: false, ...res.data });
    } catch (e) {
      setRecs({ loading: false, error: "Couldn't reach the AI service." });
    }
  };

  // Rating distribution
  const rated = reviews.filter((r) => r.rating);
  const avg = rated.length
    ? rated.reduce((a, r) => a + r.rating, 0) / rated.length
    : movie?.averageRating || 0;
  const dist = [5, 4, 3, 2, 1].map((star) => {
    const count = rated.filter((r) => Math.round(r.rating) === star).length;
    return { star, pct: rated.length ? Math.round((count / rated.length) * 100) : 0 };
  });

  const year = (movie?.releaseDate || "").slice(0, 4);

  return (
    <div className="detail-page">
      <button className="back" onClick={() => navigate("/")}>
        ← Back
      </button>

      {/* Movie detail + rating */}
      <div className="detail">
        <div className="d-poster">
          {movie?.poster && <img src={movie.poster} alt={movie.title} />}
        </div>
        <div className="d-body">
          <h1 className="d-title">{movie?.title || "…"}</h1>
          <div className="d-meta">
            {year}
            {movie?.genres?.length ? ` · ${movie.genres.join(", ")}` : ""}
          </div>
          <div className="rate-wrap">
            <div className="big-rate">
              <span className="n">{avg ? avg.toFixed(1) : "–"}</span>
              <span className="d">/ 5</span>
            </div>
            <div>
              <Stars value={avg} size={16} />
              <div className="from">
                from {reviews.length} review{reviews.length === 1 ? "" : "s"}
              </div>
            </div>
            <div className="dist">
              {dist.map((d) => (
                <div className="dist-row" key={d.star}>
                  {d.star}★
                  <div className="track">
                    <div className="fill" style={{ width: `${d.pct}%` }} />
                  </div>
                  {d.pct}%
                </div>
              ))}
            </div>
          </div>
          {movie?.trailerLink && (
            <button
              className="watch"
              onClick={() =>
                navigate(
                  `/Trailer/${movie.trailerLink.substring(
                    movie.trailerLink.length - 11
                  )}`
                )
              }
            >
              ▶ Watch trailer
            </button>
          )}
        </div>
      </div>

      {/* AI summary */}
      <div className="ai-card">
        <div className="ai-head">
          <span className="ai-ic">✦</span> AI review summary
          <span className="ai-lab">POWERED BY CLAUDE</span>
          {!summary && (
            <button className="ai-btn" onClick={loadSummary}>
              Summarize
            </button>
          )}
        </div>
        {summary?.loading && <p className="muted">Reading the reviews…</p>}
        {summary && !summary.loading && summary.configured === false && (
          <p className="muted">{summary.message}</p>
        )}
        {summary && !summary.loading && summary.error && (
          <p className="error-text">{summary.error}</p>
        )}
        {summary && !summary.loading && summary.hasData === false && (
          <p className="muted">{summary.message}</p>
        )}
        {summary && !summary.loading && summary.hasData && (
          <>
            <div className="ai-verdict">{summary.verdict}</div>
            <div className="pc">
              <div className="pc-box">
                <div className="pc-t pos">What people loved</div>
                <ul className="pos-l">
                  {(summary.pros || []).map((p, i) => (
                    <li key={i}>{p}</li>
                  ))}
                </ul>
              </div>
              <div className="pc-box">
                <div className="pc-t neg">Common criticisms</div>
                <ul className="neg-l">
                  {(summary.cons || []).map((c, i) => (
                    <li key={i}>{c}</li>
                  ))}
                </ul>
              </div>
            </div>
            <div className="mood">
              <div className="mood-lab">
                <span>Overall sentiment</span>
                <span className="mood-val">
                  {summary.sentimentLabel} · {summary.sentiment}%
                </span>
              </div>
              <div className="meter">
                <div className="pin" style={{ left: `${summary.sentiment}%` }} />
              </div>
            </div>
          </>
        )}
      </div>

      {/* Reviews + form */}
      <div className="reviews-grid">
        <div className="reviews-list">
          <div className="sec-title">
            <span className="bar" />
            Reviews
          </div>
          {reviews.length === 0 && (
            <p className="muted">No reviews yet — write the first one.</p>
          )}
          {reviews.map((r, i) => (
            <ReviewItem key={r.id || i} r={r} onUpvote={upvote} onDelete={del} />
          ))}
        </div>

        <div className="reviews-side">
          <div className="sec-title">
            <span className="bar" />
            Write a review
          </div>
          <ReviewForm onSubmit={addReview} />

          <div className="sec-title" style={{ marginTop: "1.5rem" }}>
            <span className="bar" />✦ You might also like
          </div>
          {!recs && (
            <button className="ai-btn wide" onClick={loadRecs}>
              Get AI recommendations
            </button>
          )}
          {recs?.loading && <p className="muted">Finding picks…</p>}
          {recs && !recs.loading && recs.configured === false && (
            <p className="muted">{recs.message}</p>
          )}
          {recs && !recs.loading && recs.error && (
            <p className="error-text">{recs.error}</p>
          )}
          {recs?.recommendations &&
            recs.recommendations.map((rec) => (
              <div
                className="rec"
                key={rec.movie.imdbId}
                onClick={() => navigate(`/Reviews/${rec.movie.imdbId}`)}
              >
                {rec.movie.poster && (
                  <img src={rec.movie.poster} alt={rec.movie.title} />
                )}
                <div>
                  <div className="rec-title">{rec.movie.title}</div>
                  <div className="rec-reason">{rec.reason}</div>
                </div>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
};

export default Reviews;
