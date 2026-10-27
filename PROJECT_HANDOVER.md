
# Week 3 and Week 4 — Member 2 Frontend Handover

## Assessment basis

This checklist compares the requested Member 2 frontend responsibilities with
the current repository code, available tests, and checks run on 2026-10-26.
There is no separate Week 3/Week 4 assignment document in this repository, so
the responsibility list supplied in the task is the checklist source.

Status definitions:

- **PASS**: code/test evidence supports the requirement.
- **PARTIAL**: implementation exists, but the requested behavior is not fully
  or independently verified, or there is a known configuration limitation.
- **FAIL**: the code demonstrably does not meet the requirement.
- **NOT TESTED**: no valid test/evidence was available.

## Responsibility checklist

| Requirement | Status | Repository evidence and caveats |
|---|---|---|
| React dashboard UI | PASS | React app is mounted from `frontend/src/main.jsx`; `frontend/src/App.jsx` contains authentication, dashboard, dataset, and widget views. |
| Spring Boot API integration | PARTIAL | `frontend/src/services/api.js` uses the `/api` routes and bearer auth. Vite proxies to `http://localhost:8081`. Live unauthenticated checks to both Vite's `/api/dashboard` and backend `:8081/api/dashboard` returned 401, which shows proxy reachability and protected-route behavior, not successful authenticated UI use. |
| Dashboard widgets | PASS | `App.jsx` has widget create/delete and chart-card UI; backend tests cover widget creation and order. User has reported browser persistence worked. |
| Metric selection | PASS | UI exposes COUNT, SUM, AVERAGE. Backend `DatasetServiceTest` covers COUNT, AVERAGE, sum aggregation, and invalid numeric input. |
| Chart type selection | PASS | UI exposes BAR, LINE, PIE and `ChartPreview` maps each to the corresponding Recharts component. Browser visual verification was not available in this check. |
| Drag-and-drop and reordering | PARTIAL | Native drag/drop uses `dataTransfer` and calls the reorder API; move-up/down controls are also present. Backend reordering is unit-tested. Browser automation timed out, so native drag/drop itself remains unverified. |
| Loading and error states | PARTIAL | Source implements initial dashboard loading, a retryable dashboard error state, auth form errors, dataset/widget operation errors, and upload/export busy labels. No frontend component test runner is configured, and browser automation was unavailable, so rendered behavior was not verified this turn. |
| CSV and Excel upload | PARTIAL | UI accepts `.csv` and `.xlsx` and sends multipart `file`; backend unit tests cover both formats. User previously reported successful browser upload, but it was not rerun during this verification. |
| CSV and Excel export | PARTIAL | UI requests CSV/XLSX blobs and triggers downloads; backend unit tests validate content. User previously reported successful browser exports, but downloads were not reverified this turn. |
| Persistence after refresh | PARTIAL | App reloads dashboard, dataset, widgets, and chart data from API; backend tests cover widget ordering. User previously reported refresh persistence, but browser refresh was not independently rerun during this verification. |

No requirement was confirmed FAIL from the inspected implementation. Browser-only
features are conservatively PARTIAL or NOT TESTED when they could not be tested
in the current turn.

## Frontend behavior and integration

- `frontend/src/App.jsx` owns the React UI: sign-in/sign-up form, loading and
  retry states, dataset upload/removal, widget form, chart rendering,
  drag-and-drop and keyboard-accessible move buttons, widget deletion, and
  CSV/XLSX export controls.
- `frontend/src/services/api.js` centralizes requests, JSON and multipart
  handling, auth token attachment, error conversion, 404-as-no-dataset
  handling, and binary downloads.
- `frontend/src/App.css` and `frontend/src/index.css` provide the page and
  component styling.
- `frontend/vite.config.js` proxies `/api` to `VITE_API_PROXY_TARGET` or
  `http://localhost:8081`.
- Backend endpoint, parsing, calculation, persistence, and export
  responsibilities are implemented server-side. The frontend does not parse
  or calculate the uploaded table itself.

## Intended local port configuration

For the current workspace, Vite is on port 5173 and the backend is on port
8081. Both services were listening when checked. A request to
`http://127.0.0.1:5173/api/dashboard` returned 401, matching a direct request
to backend port 8081; this confirms the proxy forwards to the listening backend
and the route requires authentication.

Spring's `application.properties` has `server.port=8080`; this differs from
Vite's existing default proxy port. Keep the current frontend configuration
and run Spring on 8081 in PowerShell:

```powershell
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

Alternatively, if Spring should remain on 8080, set the frontend proxy target
before starting/restarting Vite:

```powershell
$env:VITE_API_PROXY_TARGET = "http://localhost:8080"
npm run dev
```

Only one of these port arrangements should be used at a time. Do not set
`VITE_API_BASE_URL` to a different backend while also expecting requests to go
through the proxy. Local backend secrets and database configuration belong in
the ignored root `.env`; never put them in this file, source control, or chat.

## Manual browser verification still needed

Browser automation failed to connect to the shared browser page during this
review, so the following checks are not claimed as verified. To test safely,
use your own account and test dataset; if you already have data/widgets, avoid
replacing or deleting anything you want to keep.

### Drag-and-drop persistence

1. Start backend and frontend with matching ports as described above, then
   open `http://127.0.0.1:5173/` and sign in.
2. Ensure the dashboard has at least two widgets. If creating test widgets,
   use a disposable test account/dataset or remove only those test widgets
   afterward.
3. Drag one widget card by its header/card area onto another widget card.
4. Confirm their displayed order changes.
5. Refresh the page and confirm the same order is restored.
6. If the cards do not reorder, note browser name, whether the visual order
   changed before refresh, and any visible error text. Do not share passwords
   or tokens.

### Loading and error handling

1. Refresh while signed in and confirm the loading panel appears while the
   dashboard is being fetched, then resolves.
2. With a valid login, try loading without an uploaded dataset: the dashboard
   should remain visible and present the upload prompt rather than returning
   to sign-in.
3. To test a visible network error without changing database data, stop the
   local backend, refresh, note the dashboard error and retry control, then
   restart the backend and use Retry. Do not stop a shared service without
   permission.
4. Optionally use an invalid/unsupported file in a disposable account and
   confirm the upload error is shown. Do not replace important existing data.

### Upload, chart, exports, and refresh

1. In a disposable test account or a dashboard whose current dataset can
   safely be replaced, upload one CSV and one `.xlsx` file in separate checks.
2. Create charts using COUNT, SUM, and AVERAGE, and try BAR, LINE, PIE with
   value and optional group-by fields. Check results against known sample
   values.
3. Download both CSV and Excel results; open each and confirm headers and
   grouped values match the visible chart results.
4. Refresh and confirm dataset summary, widget definitions, chart type,
   metric, and order return.

## Automated checks run

- `.\mvnw.cmd test`: **11 tests passed; 0 failures, 0 errors, 0 skipped**.
- From `frontend/`, `npm run lint`: passed.
- From `frontend/`, `npm run build`: passed.
- `git diff --check`: passed.
- Frontend build warning: the minified JavaScript bundle exceeds Vite's
  advisory 500 kB threshold.
- Maven emits a non-failing Mockito dynamic-agent warning on the available
  Java 25 runtime.
- Frontend package scripts do not define a test command and no React
  component-testing library/test runner is configured. Adding a testing stack
  was not necessary for this review; browser automation was unavailable.

## Final completion status

The frontend implementation covers the listed features, and backend tests,
frontend lint, and build pass. Do not describe Week 3/Week 4 browser
verification as fully complete until the manual drag-and-drop and
loading/error/upload/export/refresh checks above have been performed in the
browser. Current worktree changes from other contributors were left intact;
no branch switch, commit, push, database reset, or record deletion was done.

## Final Week 1–4 review

The repository and conversation do not contain a week-by-week assignment
sheet for Weeks 1 and 2. The following milestones can be seen in Git history,
but they are not substitutes for the missing assigned acceptance criteria:

- `766744c` added User and Workspace entities/repositories.
- `ca133ee` added Dashboard and WidgetConfig entities/repositories.
- `2b7da88` added the initial React dashboard scaffold.
- `9f6ae02` added Spring Security JWT authentication.
- `ec7611d` is titled “Improve Axlero frontend dashboard.”

Therefore, completion against the actual Week 1 and Week 2 assigned
requirements is **NOT TESTED**. The commits establish that code was added, not
that it met a schedule or acceptance criteria. Week 3/4 statuses below use the
explicit Member 2 responsibilities supplied in the conversation.

| Week / requirement | Status | Evidence and limitation |
|---|---|---|
| Week 1 assigned acceptance criteria | NOT TESTED | No Week 1 requirements sheet or exact task list was present in the repository or available conversation. User/workspace entities and repositories are visible in history, but their mapping to Week 1 is unverified. |
| Week 2 assigned acceptance criteria | NOT TESTED | No Week 2 requirements sheet or exact task list was present. Dashboard/widget entities, the React scaffold, and JWT work are visible in Git history, but their mapping to Week 2 is unverified. |
| Week 3/4: React UI | PASS | React/Vite app builds and lint passes; dashboard, auth, upload, widget, and chart UI exist. |
| Week 3/4: API integration and authorization | PASS | Prior conversation records successful synthetic registration/login and protected dashboard calls against the real backend. In this final review, unauthenticated requests through Vite and directly to backend port 8081 both returned 401, confirming the proxy routes to the protected backend. No secret or personal account was used in this review. |
| Week 3/4: widget/chart creation and metrics | PASS | User reported successful browser chart creation; UI supports BAR/LINE/PIE and COUNT/SUM/AVERAGE. Backend tests cover calculations and errors. |
| Week 3/4: drag-and-drop widget order | PARTIAL | Frontend sends dragged widget ID with HTML `dataTransfer` and persists order through the reorder endpoint; backend unit tests validate ordering. The actual browser drag-and-drop interaction could not be tested because browser automation timed out. |
| Week 3/4: loading and error states | PARTIAL | Loading/retry/auth/form/upload/export error states are implemented. No frontend component tests exist and browser automation was unavailable, so rendering/interaction is not independently verified here. |
| Week 3/4: CSV upload | PASS | User reported successful browser CSV upload; backend tests also cover CSV parsing and calculation. |
| Week 3/4: Excel `.xlsx` upload | PARTIAL | Backend unit tests cover first-sheet `.xlsx` parsing. The user’s prior browser report mentioned CSV upload but did not explicitly confirm browser `.xlsx` upload; no browser retest was possible. |
| Week 3/4: CSV and Excel exports | PASS | User reported successful browser CSV/Excel exports; backend tests verify both output types. |
| Week 3/4: widget/dataset persistence after refresh | PASS | User reported chart persistence after refresh. Source reloads dashboard, dataset, widget settings/order and chart data from the API. The refresh was not repeated in this final review. |

### Architecture and data flow

`frontend/src/main.jsx` mounts the React app. `frontend/src/App.jsx` owns the
auth and dashboard interaction UI. `frontend/src/services/api.js` centralizes
HTTP requests, bearer headers, error conversion, multipart upload, and blob
downloads. Vite proxies `/api` to Spring Boot. Spring MVC controllers call
services; services use Spring Data JPA repositories to persist users,
dashboards, widget configuration, and parsed dataset rows in PostgreSQL.
`DatasetService` parses and aggregates; `WidgetExportService` serializes
calculated chart results. Recharts renders the API's `{name, value}` points.

Registration and login are public endpoints. They accept JSON and return an
`AuthResponse` with token, token type, and username on success. The frontend
stores the token in localStorage and sends it as a bearer token on protected
requests. `JwtAuthenticationFilter` validates the token and resolves the user;
`SecurityConfig` requires authentication except for auth routes and internal
error dispatches. Passwords are encoded with BCrypt.

`User` maps to `app_users`; `Dashboard` to `dashboards`; `WidgetConfig` to
`widget_configs`; and `DashboardDataset` to `dashboard_datasets`. Dataset
content is stored as JSON text associated one-to-one with a dashboard; widgets
belong to a dashboard and store their selection and position.

### API reference

All paths except `/api/auth/**` require bearer authentication.

| Method / path | Input and result |
|---|---|
| `POST /api/auth/register` | JSON `username`, `email`, `password`; returns token response with 201. |
| `POST /api/auth/login` | JSON `username`, `password`; returns token response with 200. |
| `GET /api/me` | Returns authenticated username and confirmation message. |
| `GET /api/dashboard` | Returns or creates current user's dashboard. |
| `GET /api/dashboard/widgets` | Lists saved widgets in position order. |
| `POST /api/dashboard/widgets` | JSON `title`, `chartType`, `metricType`, `valueField`, optional `groupByField`; creates widget. |
| `PUT /api/dashboard/widgets/order` | JSON `{ "widgetIds": [...] }`; IDs must include every widget exactly once. |
| `DELETE /api/dashboard/widgets/{widgetId}` | Deletes widget; 204 on success. |
| `GET /api/dashboard/widgets/{widgetId}/data` | Returns aggregated `{name, value}` points. |
| `GET /api/dashboard/dataset` | Returns dataset filename, columns, row count; 404 if none uploaded. |
| `POST /api/dashboard/dataset` | Multipart `file`; accepts CSV or `.xlsx`, replaces the dashboard's active dataset. |
| `DELETE /api/dashboard/dataset` | Deletes active dataset; saved widgets remain; 204 on success. |
| `GET /api/dashboard/export.csv` | Downloads calculated widget results as CSV. |
| `GET /api/dashboard/export.xlsx` | Downloads calculated widget results as `.xlsx`. |

The backend limits uploads to 10 MB, 100 columns, and 50,000 data rows. Excel
processing uses the first worksheet only. CSV and XLSX export rows contain
Widget, Metric, Chart type, Group, and Value.

### Setup, ports, and secrets

Root `.env` is imported by `spring.config.import=optional:file:.env[.properties]`
and is ignored by `.gitignore`. It contains local PostgreSQL settings and a
required random `JWT_SECRET`. Do not commit or share it.

For this workspace the Vite proxy default and active backend use port 8081,
while `application.properties` defaults Spring to 8080. Start this project
with one matching arrangement. The README now documents the workspace option:

```powershell
$env:SERVER_PORT = "8081"; .\mvnw.cmd spring-boot:run
```

The alternative is leaving Spring at 8080 and setting
`VITE_API_PROXY_TARGET=http://localhost:8080` before Vite starts. Do not
configure the proxy and backend to different ports.

### Contribution attribution

The current branch is `feature/neha-dashboard-integration` at `315023d`.
Git history attributes the initial frontend scaffold to Neha Bora
(`2b7da88`) and records a later frontend improvement (`ec7611d`). JWT,
dashboard entities, and later dashboard backend work appear in commits by
multiple contributors. Current frontend/backend edits in the worktree are
uncommitted; Git cannot establish who authored each uncommitted line. This
review describes the implementation, not personal authorship. No commit,
push, branch switch, or database deletion was performed.

## Final verification on 2026-10-26

- `.\mvnw.cmd test`: **11 passed, 0 failed, 0 errors, 0 skipped**.
- `frontend`: `npm run lint` passed.
- `frontend`: `npm run build` passed; Vite reports a non-failing main bundle
  size warning (about 638 kB minified, advisory threshold 500 kB).
- `git diff --check` passed.
- `.env` exists, is matched by `.gitignore`, is ignored (`git status --ignored`
  shows it as ignored), and is not tracked by Git. Its contents were never
  displayed or read for this review.
- A direct unauthenticated request to backend port 8081 and a request through
  Vite port 5173 both returned 401 for the protected dashboard route.
- Browser automation repeatedly timed out connecting to the shared page.
  Consequently, drag-and-drop, loading/error presentation, and the user's
  flows were not independently rerun in this final pass; the PASS entries
  explicitly rely on the user's earlier browser confirmation or prior
  conversation verification.

The test suite has backend unit/service tests, not full Spring/PostgreSQL or
React/browser integration tests. Mockito emitted a non-failing dynamic-agent
warning. The only schedule-level incompleteness established from available
information is the missing Week 1/2 assignment criteria; do not invent
additional Week 5 work.

## Team-lead status and demo checklist

**Team-lead summary:** The dashboard's assigned Week 3/4 frontend features
are implemented. Backend tests (11), frontend lint/build, and diff checks pass.
The user previously confirmed CSV upload, chart creation, CSV/Excel export,
and refresh persistence in their browser. API reachability/authentication has
also been exercised. Native drag-and-drop and loading/error UI could not be
retested because browser automation was unavailable, and `.xlsx` browser
upload was not explicitly confirmed. Weeks 1/2 cannot be scored against
assigned requirements because their task sheets are not in the available
repository/conversation. The workspace uses port 8081 with an explicit
override from Spring's 8080 default.

**Demo checklist:**

1. Start backend and frontend with both using port 8081, then open
   `http://127.0.0.1:5173/`.
2. Register or sign in; demonstrate the visible loading state and dashboard.
3. Upload a test CSV, create a widget, and explain its selected metric,
   chart type, and optional group-by field.
4. Create two widgets, drag one onto the other, and confirm order after refresh.
5. Demonstrate CSV and Excel downloads and compare their result rows with the
   chart values.
6. Refresh and confirm dataset and widget configuration restore.
7. In a disposable account only, demonstrate an upload/network error message.
   Do not replace or delete important account data during the demo.
