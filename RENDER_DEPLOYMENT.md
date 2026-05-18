# ConceptClarity Render Docker Deployment Guide

ConceptClarity is now configured as one Dockerized Spring Boot web service. The Docker build copies the static frontend into Spring Boot, so the same Render URL serves both:

- Frontend pages: `/`, `/login.html`, `/dashboard.html`, `/history.html`
- Backend API: `/api/...`

## Local Docker Run

```bash
docker compose up --build
```

Open:

```text
http://localhost:18080
```

Health check:

```text
http://localhost:18080/api/health
```

## Render Setup

1. Push this repository to GitHub.
2. In Render, create a PostgreSQL database.
3. In Render, create a new Web Service.
4. Select the GitHub repository.
5. Set Language to `Docker`.
6. Set Dockerfile Path to `./Dockerfile`.
7. Add environment variables from the list below.
8. Deploy.

## Render Environment Variables

Use the internal PostgreSQL host from Render when possible. Render Postgres exposes connection details in the database dashboard.

```text
PORT=8080
DB_URL=jdbc:postgresql://INTERNAL_HOST:5432/DATABASE_NAME
DB_USERNAME=DATABASE_USER
DB_PASSWORD=DATABASE_PASSWORD
APP_CORS_ALLOWED_ORIGINS=https://your-render-service.onrender.com
JPA_DDL_AUTO=validate
FLYWAY_ENABLED=true
JPA_SHOW_SQL=false
JPA_FORMAT_SQL=false
JAVA_OPTS=-XX:MaxRAMPercentage=75
```

Do not paste Render's `postgresql://...` URL directly into `DB_URL`. Spring JDBC expects this format:

```text
jdbc:postgresql://host:5432/database
```

## Git Commands

```bash
git init
git add .
git commit -m "Dockerize app and migrate to PostgreSQL"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/ConceptClarity.git
git push -u origin main
```

## Common Render Fixes

- Port error: keep `server.port=${PORT:8080}` and set `PORT=8080`.
- Database error: make sure `DB_URL` starts with `jdbc:postgresql://`.
- Migration error: use a fresh Render PostgreSQL database or repair the Flyway migration history after failed experiments.
- CORS error: add the exact Render service URL to `APP_CORS_ALLOWED_ORIGINS`.
- Build error: confirm Render service Language is `Docker` and Dockerfile Path is `./Dockerfile`.
