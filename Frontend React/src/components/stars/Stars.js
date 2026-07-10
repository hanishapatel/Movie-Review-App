import React from "react";

// Read-only or interactive 5-star rating.
const Stars = ({ value = 0, size = 15, interactive = false, onChange }) => {
  return (
    <span className="stars" style={{ fontSize: `${size}px` }}>
      {[1, 2, 3, 4, 5].map((s) => (
        <span
          key={s}
          className={s <= Math.round(value) ? "star on" : "star"}
          role={interactive ? "button" : undefined}
          aria-label={interactive ? `${s} star${s > 1 ? "s" : ""}` : undefined}
          style={interactive ? { cursor: "pointer" } : undefined}
          onClick={interactive ? () => onChange && onChange(s) : undefined}
        >
          ★
        </span>
      ))}
    </span>
  );
};

export default Stars;
