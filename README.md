# Axlero Dynamic Dashboard

Spring Boot 3.5 / Java 21 API with JWT authentication and a React 19/Vite dashboard.

## Local setup

Requirements: Java 21+, Node.js, npm, and a running PostgreSQL instance. The
application uses PostgreSQL; it does not silently substitute an in-memory
database.

1. Create the `axlero_dashboard` database in PostgreSQL.
2. Copy `.env.example` to `.env`. Set the datasource URL, username, and
   password to the values configured for your local PostgreSQL instance. Set
   `JWT_SECRET` to a newly generated random key containing at least 32 bytes.
   Keep `.env` private; it is ignored by Git. Generate the key locally with a
   password manager or secure random generator and enter it directly in `.env`;
   do not paste it into chat or commit it.
3. In PowerShell, start the backend on port 8081 to match the Vite proxy:
   `$env:SERVER_PORT = "8081"; .\mvnw.cmd spring-boot:run`
4. In another terminal, run `npm ci` from `frontend\`, then
   `npm run dev`. Vite proxies `/api` requests to `http://localhost:8081`.

Spring Boot's `application.properties` defaults to port 8080. To keep the
backend on 8080 instead, set `VITE_API_PROXY_TARGET=http://localhost:8080`
before starting Vite. Use one matching backend/proxy port configuration.

The API also accepts `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, and `JWT_SECRET` as process environment variables.
Do not use development credentials or the local JWT key in production.

## Dashboard behavior

Registration/login and the authenticated dashboard API use PostgreSQL. Each
dashboard can store one active CSV or `.xlsx` dataset (up to 10 MB, 100 columns,
and 50,000 data rows); uploading another file replaces it after confirmation.
The dashboard uses the uploaded column names to configure COUNT, SUM, and
AVERAGE charts grouped by an optional column. Widget settings, dataset content,
and ordering are persisted per dashboard. CSV and Excel exports contain the
calculated chart results.

## Checks

- Frontend: `npm run lint` and `npm run build` from `frontend\`.
- Backend: `.\mvnw.cmd clean test` from the repository root.
