import React from "react";
import { useNavigate } from "react-router-dom";

const MovieCard = ({ movie, rank }) => {
  const navigate = useNavigate();
  const rating = movie.averageRating || 0;
  const year = (movie.releaseDate || "").slice(0, 4);
  const genre = movie.genres && movie.genres.length ? movie.genres[0] : "";

  return (
    <div className="mcard" onClick={() => navigate(`/Reviews/${movie.imdbId}`)}>
      <div className="mcard-art">
        {movie.poster ? (
          <img src={movie.poster} alt={movie.title} loading="lazy" />
        ) : (
          <div className="mcard-noart">{movie.title}</div>
        )}
        {rank ? <span className="mcard-rank">#{rank}</span> : null}
        {rating > 0 && <span className="mcard-rt">★ {rating.toFixed(1)}</span>}
      </div>
      <div className="mcard-nm">{movie.title}</div>
      <div className="mcard-yr">
        {year}
        {genre ? ` · ${genre}` : ""}
      </div>
    </div>
  );
};

export default MovieCard;
