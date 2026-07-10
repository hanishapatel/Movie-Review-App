import "./App.css";
import api from "./api/axiosConfig";
import { useState, useEffect, useCallback } from "react";
import Layout from "./components/layout";
import { Routes, Route } from "react-router-dom";
import Home from "./components/home/Home";
import Header from "./components/header/Header";
import Trailer from "./components/trailer/Trailer";
import Reviews from "./components/reviews/Reviews";
import NotFound from "./components/notFound/NotFound";

function App() {
  const [movies, setMovies] = useState([]);
  const [genres, setGenres] = useState([]);
  const [trending, setTrending] = useState([]);
  const [genre, setGenre] = useState("All");
  const [sort, setSort] = useState("rating");
  const [ai, setAi] = useState(null); // null | { loading, results, explanation, error, message, query }

  const [movie, setMovie] = useState(null);
  const [reviews, setReviews] = useState([]);

  const getMovies = useCallback(async () => {
    try {
      const params = {};
      if (genre && genre !== "All") params.genre = genre;
      if (sort) params.sort = sort;
      const res = await api.get("/api/v1/movies", { params });
      setMovies(res.data);
    } catch (err) {
      console.log(err);
    }
  }, [genre, sort]);

  useEffect(() => {
    getMovies();
  }, [getMovies]);

  useEffect(() => {
    api.get("/api/v1/movies/genres").then((r) => setGenres(r.data)).catch(() => {});
    api.get("/api/v1/movies/trending").then((r) => setTrending(r.data)).catch(() => {});
  }, []);

  const getMovieData = async (movieId) => {
    try {
      const res = await api.get(`/api/v1/movies/${movieId}`);
      const singleMovie = res.data;
      setMovie(singleMovie);
      setReviews(singleMovie.reviewIds || []); // API returns reviewIds, not reviews
    } catch (error) {
      console.error(error);
    }
  };

  const aiSearch = async (query) => {
    setAi({ loading: true, query });
    try {
      const res = await api.post("/api/v1/ai/search", { query });
      setAi({
        loading: false,
        query,
        results: res.data.results || [],
        explanation: res.data.explanation,
        configured: res.data.configured,
        message: res.data.message,
        error: res.data.error,
      });
    } catch (e) {
      setAi({ loading: false, query, error: "Couldn't reach the AI search service." });
    }
  };

  const clearAi = () => setAi(null);

  return (
    <div className="App">
      <Header />
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route
            path="/"
            element={
              <Home
                movies={movies}
                trending={trending}
                genres={genres}
                genre={genre}
                setGenre={setGenre}
                sort={sort}
                setSort={setSort}
                ai={ai}
                aiSearch={aiSearch}
                clearAi={clearAi}
              />
            }
          />
          <Route path="/Trailer/:ytTrailerId" element={<Trailer />} />
          <Route
            path="/Reviews/:movieId"
            element={
              <Reviews
                getMovieData={getMovieData}
                movie={movie}
                reviews={reviews}
              />
            }
          />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </div>
  );
}

export default App;
