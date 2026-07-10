import axios from "axios";

// export default axios.create({
//   baseURL: "https://9c96-103-106-239-104.ap.ngrok.io",
//   headers: { "ngrok-skip-browser-warning": "true" },
// });

export default axios.create({
  // Set REACT_APP_API_URL to the deployed backend URL in production.
  baseURL: process.env.REACT_APP_API_URL || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
});
