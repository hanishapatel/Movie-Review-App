# 🎬 Gold — AI-Powered Movie Reviews

A full-stack movie review platform where audiences rate films, write social reviews,
and get **Claude-powered** insights — an AI summary of every film's reviews, a
sentiment meter, natural-language search, and personalized recommendations.

**Stack:** Spring Boot · MongoDB · React · Anthropic (Claude) API

---

## ✨ Features

**Ratings & social reviews**
- 1–5 star ratings with a per-film average and a rating-distribution breakdown
- "Helpful" upvotes, spoiler tags with blur-to-reveal, edit and delete

**AI superpowers (Claude)**
- **Review summary** — pros/cons and a one-line verdict distilled from all reviews
- **Sentiment meter** — overall audience mood as a 0–100 score
- **Natural-language search** — e.g. *"feel-good sci-fi with a strong lead"*
- **Recommendations** — "you might also like", with a reason for each pick

**Discovery**
- Genre filter chips, sort (top rated / most reviewed / A–Z / newest), and a Trending row

---

## 🗂️ Project structure

```
Backend Spring/    Spring Boot + MongoDB REST API (Java 17)
Frontend React/    Create React App single-page frontend
```

---

## 🚀 Getting started

### Prerequisites
- **Java 17** and the bundled Maven wrapper (`./mvnw`)
- **Node.js 18+**
- A **MongoDB** database (e.g. free MongoDB Atlas)
- *(Optional)* an **Anthropic API key** for the AI features

### 1. Configure the backend
Create `Backend Spring/src/main/resources/.env` (git-ignored) from the template:

```bash
cp "Backend Spring/src/main/resources/.env.example" "Backend Spring/src/main/resources/.env"
```

Fill it in:

```dotenv
MONGO_URI=mongodb+srv://<user>:<password>@<cluster>.mongodb.net/?retryWrites=true&w=majority
MONGO_DATABASE=movielist
ANTHROPIC_API_KEY=sk-ant-...   # optional — leave blank to run without AI
```

### 2. Run the backend  →  http://localhost:8080
```bash
cd "Backend Spring"
./mvnw spring-boot:run
```

### 3. Run the frontend  →  http://localhost:3000
```bash
cd "Frontend React"
npm install
npm start
```

> Without an `ANTHROPIC_API_KEY`, everything works except the AI features, which
> show a friendly "AI is off" message instead of erroring.

---

## 🛠️ Tech
Spring Boot 3 · Spring Data MongoDB · Anthropic Java SDK · React 18 · React Router · Axios
