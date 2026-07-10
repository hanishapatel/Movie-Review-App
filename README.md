# 🎬 Gold — AI-Powered Movie Reviews

A full-stack movie review app with star ratings, social reviews, and
**Claude-powered** AI: review summaries, sentiment, natural-language search,
and recommendations.

**Stack:** Spring Boot · MongoDB · React · Anthropic (Claude) API

## Run it

Create `Backend Spring/src/main/resources/.env` from `.env.example` and set
`MONGO_URI`, `MONGO_DATABASE`, and (optionally) `ANTHROPIC_API_KEY`.

```bash
# backend  → http://localhost:8080
cd "Backend Spring" && ./mvnw spring-boot:run

# frontend → http://localhost:3000
cd "Frontend React" && npm install && npm start
```

Without an `ANTHROPIC_API_KEY` everything works except the AI features.

## Notes
- Credentials load from a git-ignored `.env` — never commit real values.
