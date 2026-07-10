import React, { useState } from "react";
import Stars from "../stars/Stars";

const ReviewForm = ({ onSubmit }) => {
  const [rating, setRating] = useState(0);
  const [author, setAuthor] = useState("");
  const [body, setBody] = useState("");
  const [spoiler, setSpoiler] = useState(false);

  const submit = (e) => {
    e.preventDefault();
    if (!body.trim() || rating === 0) return;
    onSubmit({ rating, author: author.trim(), body: body.trim(), spoiler });
    setRating(0);
    setAuthor("");
    setBody("");
    setSpoiler(false);
  };

  return (
    <form className="review-form" onSubmit={submit}>
      <div className="rf-row">
        <span className="rf-label">Your rating</span>
        <Stars value={rating} size={22} interactive onChange={setRating} />
      </div>
      <input
        className="rf-input"
        placeholder="Your name (optional)"
        value={author}
        onChange={(e) => setAuthor(e.target.value)}
      />
      <textarea
        className="rf-textarea"
        rows={3}
        placeholder="Share your thoughts on this film…"
        value={body}
        onChange={(e) => setBody(e.target.value)}
      />
      <label className="rf-spoiler">
        <input
          type="checkbox"
          checked={spoiler}
          onChange={(e) => setSpoiler(e.target.checked)}
        />
        This review contains spoilers
      </label>
      <button type="submit" className="rf-submit" disabled={rating === 0 || !body.trim()}>
        Post review
      </button>
    </form>
  );
};

export default ReviewForm;
