# Axlero Dynamic Data Dashboard Builder

A Spring Boot-based backend application for building and managing dynamic data dashboards. The application provides REST APIs for user authentication, workspace management, dashboard management, and widget configuration.

## 🚀 Project Overview

The **Axlero Dynamic Data Dashboard Builder** aims to provide a centralized platform where users can organize workspaces, create dashboards, and configure visualization widgets.

The current backend includes JWT-based authentication and REST APIs for managing workspaces, dashboards, and dashboard widgets.

## ✨ Features

- **User Authentication:** User registration and login.
- **JWT Security:** Token-based authentication for protected API endpoints.
- **Workspace Management:** Create and retrieve workspaces.
- **Dashboard Management:** Create, retrieve, update, and delete dashboards through the available API endpoints.
- **Widget Management:** Create, retrieve, update, and delete dashboard widgets.
- **RESTful APIs:** HTTP endpoints for interacting with backend resources.
- **PostgreSQL Integration:** Persistent relational data storage.
- **Validation:** Request validation for required fields and supported field lengths.

## 🛠️ Technology Stack

| Technology | Purpose |
|---|---|
| Java 21 | Programming language |
| Spring Boot 3.5.6 | Backend framework |
| Spring Security | Application security |
| JWT | Token-based authentication |
| Spring Data JPA / Hibernate | Database persistence |
| PostgreSQL | Relational database |
| Maven | Build and dependency management |
| Git and GitHub | Version control and collaboration |
| IntelliJ IDEA | Development environment |

## 📋 Prerequisites

Install the following before running the application:

- Java Development Kit (JDK) 21
- PostgreSQL
- Git
- IntelliJ IDEA or another Java IDE

The project includes a Maven wrapper, so a separate Maven installation may not be necessary.

## ⚙️ Installation and Setup

### 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd axlero-dashboard-git
```

Replace the placeholder with your actual repository URL.

### 2. Configure PostgreSQL

Create the application database:

```sql
CREATE DATABASE axlero_dashboard;
```

Make sure PostgreSQL is running and that your database username and password are correct.

### 3. Configure environment variables

The application uses environment variables for sensitive configuration values.

In Windows PowerShell, set:

```powershell
$env:SPRING_DATASOURCE_PASSWORD = "YOUR_POSTGRES_PASSWORD"
$env:JWT_SECRET = "YOUR_SECURE_JWT_SECRET"
```

Use your actual PostgreSQL password. Set a strong, private JWT secret suitable for your environment.

Keep credentials out of source control. Do not commit real passwords, tokens, or production secrets.

### 4. Run the application

From the project root, execute:

```powershell
.\mvnw.cmd spring-boot:run
```

The backend runs at:

```text
http://localhost:8080
```

To compile the project:

```powershell
.\mvnw.cmd clean compile
```

## 🔐 Authentication Workflow

1. Register a user through the registration endpoint.
2. Log in with the registered credentials.
3. Receive a JWT token.
4. Include the token in requests to protected endpoints.

Example authorization header:

```http
Authorization: Bearer <YOUR_JWT_TOKEN>
```

## 📡 API Endpoints

All endpoints use the base URL `http://localhost:8080`.

### Authentication

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Register a user |
| POST | `/api/auth/login` | Authenticate a user and obtain a JWT |

### Workspaces

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/workspaces` | Create a workspace |
| GET | `/api/workspaces` | Retrieve workspaces available to the authenticated user |

### Dashboards

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/workspaces/{workspaceId}/dashboards` | Create a dashboard in a workspace |
| GET | `/api/workspaces/{workspaceId}/dashboards` | List dashboards in a workspace |
| GET | `/api/dashboards/{dashboardId}` | Retrieve a dashboard |
| PUT | `/api/dashboards/{dashboardId}` | Update a dashboard |
| DELETE | `/api/dashboards/{dashboardId}` | Delete a dashboard |

### Dashboard Widgets

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/api/dashboards/{dashboardId}/widgets` | Create a widget |
| GET | `/api/dashboards/{dashboardId}/widgets` | List widgets in a dashboard |
| PUT | `/api/widgets/{widgetId}` | Update a widget |
| DELETE | `/api/widgets/{widgetId}` | Delete a widget |

Protected endpoints require a valid JWT in the authorization header.

## 🧩 Widget Configuration

A widget creation or update request accepts the following JSON fields:

```json
{
  "title": "Monthly Sales",
  "chartType": "BAR",
  "metricType": "SUM",
  "groupByField": "month",
  "valueField": "sales",
  "position": 0
}
```

| Field | Description |
|---|---|
| `title` | Widget title |
| `chartType` | Chart type configuration |
| `metricType` | Metric or aggregation configuration |
| `groupByField` | Field used for grouping |
| `valueField` | Field used for values |
| `position` | Widget position, starting from zero |

These fields configure the widget. Actual chart rendering and data aggregation depend on the corresponding functionality being implemented.

## 🧪 Testing the APIs

You can test the backend using PowerShell, Postman, or another HTTP client.

Recommended testing sequence:

1. Register a user.
2. Log in and obtain a JWT token.
3. Create a workspace.
4. Create a dashboard in that workspace.
5. Create a widget in the dashboard.
6. Retrieve the widget list.
7. Update the widget configuration.
8. Delete the widget and verify that it is no longer listed.

The authentication, workspace, dashboard creation/listing, and widget CRUD flows have been tested during development.

## 📁 Project Structure

The backend follows a layered Spring Boot architecture.

```text
src/
└── main/
    ├── java/
    │   └── com/axlero/dashboard/
    │       ├── controller/
    │       ├── dto/
    │       ├── service/
    │       ├── security/
    │       └── ...
    └── resources/
        └── application.properties
```

- **controller:** REST API endpoints.
- **dto:** Request and response data transfer objects.
- **service:** Application business logic.
- **security:** Authentication and authorization components.
- **resources:** Application configuration.

The exact package contents may evolve as the project develops.

## 🔒 Security Notes

- Store database passwords and JWT secrets in environment variables.
- Never commit real credentials or JWT tokens.
- Use HTTPS and appropriate secret management in production.
- Validate authentication and authorization for every protected resource.
- Use a strong JWT secret and configure token expiration appropriately.

## 🛣️ Future Enhancements

Potential future improvements include:

- Dynamic chart rendering using real data.
- CSV and other data-source integrations.
- Dashboard visualization and filtering.
- Frontend integration.
- Improved error handling and API documentation.
- Automated unit and integration tests.
- Deployment and production configuration.

These are planned enhancements and are not represented as completed features.

## 👨‍💻 Development

This project is being developed as part of the Axlero internship project.

**Project:** Axlero Dynamic Data Dashboard Builder  
**Backend:** Java and Spring Boot  
**Database:** PostgreSQL

## 📄 License

A license has not yet been specified. Add a license before distributing the project if required.
