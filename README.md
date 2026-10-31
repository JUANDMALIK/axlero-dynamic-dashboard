# Axlero Dynamic Dashboard

Axlero is a Spring Boot 3.5 / Java 21 API and React 19/Vite dashboard for
authenticated workspaces, data uploads, dashboard widgets, and chart exports.
PostgreSQL is the application database.

## Requirements

- Java 21+
- Node.js and npm
- PostgreSQL

## Local setup

1. Create the `axlero_dashboard` database in PostgreSQL.
2. Copy `.env.example` to `.env`. Set the datasource URL, username, and
   password for your local database, and provide a newly generated `JWT_SECRET`
   containing at least 32 bytes. Keep `.env` private; it is ignored by Git.
   Never paste credentials or a JWT secret into chat or commit them.
3. In PowerShell, start the backend:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. In another terminal, install frontend dependencies and start Vite:

   ```powershell
   cd frontend
   npm ci
   npm run dev
   ```

The backend defaults to port 8080. The Vite dev server proxies both `/api` and
`/dashboard` to the backend and defaults to port 8081; either set
`SERVER_PORT=8081` before starting Spring Boot, or configure
`VITE_API_PROXY_TARGET=http://localhost:8080` before starting Vite. Keep the
backend port and proxy target in sync.

The backend also reads `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`, and `APP_CORS_ALLOWED_ORIGINS` from
the process environment. Never use development credentials or a local JWT key
in production.

## Features

- JWT registration and login, stateless protected APIs, and configurable CORS.
- Workspace creation and listing for the authenticated user through API endpoints;
  workspace management is not currently exposed in the dashboard UI.
- Dashboard summary and CSV/Excel dashboard configuration exports.
- Dashboard widget creation, deletion, ordering, chart data, and CSV/XLSX
  result exports.
- CSV and `.xlsx` dataset upload for widget charts. Each dashboard stores one
  active integration dataset (up to 10 MB, 100 columns, and 50,000 data rows);
  replacing it updates chart results.
- A compatible CSV analytics API with strict UTF-8/CSV validation and cached
  summary analytics (up to 100,000 data rows).
- COUNT, SUM, and AVERAGE chart metrics grouped by an optional dataset column.

The widget-chart API and legacy CSV analytics API are retained as separate
compatibility surfaces and use separate persisted dataset records.
The current dashboard UI uses the widget-chart APIs; the dashboard summary and
legacy CSV analytics endpoints remain available as backend APIs but are not
currently displayed in the UI. Workspace creation and listing are also API-only.
Legacy widget replacement updates only legacy widgets and leaves chart-builder
widgets unchanged.

## API overview

Protected endpoints require `Authorization: Bearer <token>`.

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Authenticate and obtain a JWT |
| GET, POST | `/api/workspaces` | List or create the current user's workspaces |
| GET | `/dashboard` | Get the current user's dashboard summary |
| GET | `/dashboard/export/csv` | Export dashboard widget configuration as CSV |
| GET | `/dashboard/export/excel` | Export dashboard widget configuration as XLSX |
| GET | `/api/dashboard` | Get or create the current user's dashboard |
| GET, PUT | `/api/dashboard/widgets` | List chart-builder widgets or replace legacy widgets without modifying chart-builder widgets |
| POST | `/api/dashboard/widgets` | Create a chart widget |
| PUT | `/api/dashboard/widgets/order` | Update widget order |
| DELETE | `/api/dashboard/widgets/{widgetId}` | Delete a chart widget |
| GET | `/api/dashboard/widgets/{widgetId}/data` | Get aggregated chart data |
| GET, POST, DELETE | `/api/dashboard/dataset` | Retrieve, upload, or remove a CSV/XLSX chart dataset |
| GET | `/api/dashboard/export.csv` | Export calculated chart results as CSV |
| GET | `/api/dashboard/export.xlsx` | Export calculated chart results as XLSX |
| POST | `/api/datasets/upload` | Upload a UTF-8 CSV for legacy analytics |
| GET | `/api/datasets/latest` | Get metadata for the latest legacy CSV |
| GET | `/api/datasets/latest/analytics` | Get cached statistics and chart-ready CSV analytics |

## Validation

Run from the repository root:

```powershell
.\mvnw.cmd test
```

Run from `frontend\`:

```powershell
npm run lint
npm run build
```
