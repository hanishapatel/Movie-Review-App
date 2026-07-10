import React from "react";
import { Link } from "react-router-dom";

const Header = () => {
  return (
    <header className="topbar">
      <Link to="/" className="brand">
        <span className="brand-dot">🎬</span>
        <span>Gold</span>
      </Link>
      <span className="tagline">Movie reviews, powered by AI</span>
    </header>
  );
};

export default Header;
