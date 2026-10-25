# Dynamic Data Dashboard Builder

Spring Boot APIs and a React dashboard studio for building and arranging metric visualizations. The application includes user registration, session-based login, and owner-scoped dashboard configuration APIs.

## Requirements

- Java 17+
- Maven (or the included Maven wrapper)
- Node.js 18+ and npm (for the React frontend)

## Run the application

From the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

The API is available at `http://localhost:8080`. The default development database is H2, stored in `./data/dashboarddb`. The H2 console is available at `http://localhost:8080/h2-console`.

## Authentication API

Authentication uses an HTTP session. A successful login sets the `JSESSIONID` cookie; send that cookie on subsequent authenticated requests. Passwords are stored using BCrypt and are never included in API responses.

Because the API uses cookie-based sessions, mutating requests require a CSRF token. Fetch one first:

```http
GET /api/auth/csrf
```

The response contains `headerName` and `token`; the response also sets the CSRF cookie. Send the returned token using the header name in `headerName` (normally `X-XSRF-TOKEN`) on each `POST`. Keep the cookies from the CSRF and login responses. Postman automatically retains cookies for the same host.

### Register

```http
POST /api/auth/register
Content-Type: application/json
X-XSRF-TOKEN: <token>

{
  "name": "Ada Lovelace",
  "email": "ada@example.com",
  "password": "strong-password"
}
```

Returns `201 Created` with the new user's `id`, `name`, and normalized lowercase `email`. Name and email are required; password must be at least 8 characters and no more than 72 UTF-8 bytes. Email is unique regardless of case.

### Log in

```http
POST /api/auth/login
Content-Type: application/json
X-XSRF-TOKEN: <token>

{
  "email": "ada@example.com",
  "password": "strong-password"
}
```

Returns `200 OK` with the user profile and sets the authenticated `JSESSIONID` session cookie.

### Get the current user

```http
GET /api/auth/me
Cookie: JSESSIONID=<session-id>
```

Returns `200 OK` with the authenticated user's profile. Requests without a valid session are denied.

### Log out

```http
POST /api/auth/logout
Cookie: JSESSIONID=<session-id>
X-XSRF-TOKEN: <token>
```

Returns `204 No Content` and invalidates the session.

### Error responses

- `400 Bad Request`: invalid JSON or validation failure.
- `401 Unauthorized`: invalid email/password on login.
- `403 Forbidden`: missing/invalid CSRF token or unauthenticated request to a protected endpoint.
- `409 Conflict`: email already registered.

## Dashboard API

Dashboard endpoints require an authenticated session. Dashboard reads are scoped to the logged-in user, and every create, update, or delete request requires the CSRF token described above.

- `GET /api/dashboards` lists the current user's dashboards and widget configurations.
- `POST /api/dashboards` creates a dashboard.
- `GET /api/dashboards/{id}` reads one of the current user's dashboards.
- `PUT /api/dashboards/{id}` replaces the dashboard name and its widget configurations.
- `DELETE /api/dashboards/{id}` deletes the dashboard and its widgets.

Dashboard bodies contain a `name` and a `widgets` array. A widget has `title`, `chartType` (`area`, `bar`, `line`, or `pie`), `metricType` (`sum`, `average`, or `count`), `groupByField`, `valueField`, and zero-based `position`. Updates replace the full widget list; dashboards not owned by the current user return `404 Not Found`.

## React dashboard

The React/Vite application lives in [`frontend/`](./frontend). Start the Spring Boot API in one terminal, then run:

```powershell
cd frontend
npm install
npm run dev
```

Open the Vite URL printed in the terminal. The development server proxies `/api` requests to `http://localhost:8080`; sign in or register using the backend session APIs. Create and arrange widgets with the dashboard builder and use **Save changes** to persist them. The chart series and summary cards are clearly labeled sample metrics because no business-data source API is configured yet.

Build the frontend assets with `npm run build` from `frontend/`.

## Postman API testing

Import [`postman/axlero-dashboard-auth.postman_collection.json`](./postman/axlero-dashboard-auth.postman_collection.json) into Postman and run the collection in order. It fetches a CSRF token, generates a unique email, registers a user, logs in, checks the current-user response, and logs out. Set the collection variable `baseUrl` if the server is not running at `http://localhost:8080`.

Automated tests exercise these API responses as well as password hashing, duplicate registration, input validation, session persistence, logout, and real HTTP CSRF behavior.

## Run tests

```powershell
.\mvnw.cmd test
```
