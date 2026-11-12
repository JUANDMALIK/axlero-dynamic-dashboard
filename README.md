# 

# \# Axlero Dynamic Dashboard

# 

# Axlero Dynamic Dashboard is a full-stack web application built using React and Spring Boot. It provides user authentication, dashboard management, widgets, dataset uploads, chart visualizations, analytics, and data exports.

# 

# The application uses PostgreSQL for persistent storage and JWT-based authentication for secure API access.

# 

# \## Features

# 

# \- User registration and login.

# \- JWT-based authentication.

# \- BCrypt password encryption.

# \- Stateless REST APIs.

# \- Workspace creation and listing.

# \- Dashboard summary and management.

# \- Create, retrieve, delete, and reorder dashboard widgets.

# \- CSV and XLSX dataset uploads.

# \- Widget-based chart data and visualizations.

# \- Dataset analytics.

# \- CSV and Excel exports.

# 

# \## Technology Stack

# 

# \### Frontend

# 

# | Technology | Version |

# |---|---|

# | React | 19.3.1 |

# | React DOM | 19.3.1 |

# | Vite | 8.3.4 |

# | Recharts | 3.10.1 |

# | Lucide React | 1.59.0 |

# | JavaScript | ES Modules |

# 

# \### Backend

# 

# | Technology | Version / Configuration |

# |---|---|

# | Java | 21 |

# | Spring Boot | 3.5.6 |

# | Spring Security | Spring Boot managed |

# | Spring Data JPA | Spring Boot managed |

# | Database | PostgreSQL |

# | Authentication | JWT |

# | Password Encoding | BCrypt |

# 

# \## System Architecture

# 

# The application follows a frontend and backend architecture.

# 

# 1\. React provides the user interface.

# 2\. Vite runs the frontend during development.

# 3\. Spring Boot handles REST API requests.

# 4\. Spring Security and JWT handle authentication.

# 5\. PostgreSQL stores application data.

# 

# ```text

# React Frontend

# &#x20;     |

# &#x20;     v

# &#x20;  Vite Server

# &#x20;     |

# &#x20;     v

# Spring Boot Backend

# &#x20;     |

# &#x20;     +---- Authentication

# &#x20;     |

# &#x20;     +---- Dashboard

# &#x20;     |

# &#x20;     +---- Widget Management

# &#x20;     |

# &#x20;     +---- Dataset Management

# &#x20;     |

# &#x20;     +---- Analytics and Exports

# &#x20;     |

# &#x20;     v

# &#x20;  PostgreSQL

# ```

# 

# \## Requirements

# 

# Install the following software:

# 

# \- JDK 21 or later

# \- Node.js and npm

# \- PostgreSQL

# \- Git

# 

# Check the installed versions:

# 

# ```powershell

# java -version

# node --version

# npm --version

# git --version

# ```

# 

# \## Installation and Setup

# 

# \### 1. Clone the Repository

# 

# ```powershell

# git clone https://github.com/JUANDMALIK/axlero-dynamic-dashboard.git

# cd axlero-dynamic-dashboard

# ```

# 

# \### 2. Create PostgreSQL Database

# 

# Create a database named:

# 

# ```sql

# CREATE DATABASE axlero\_dashboard;

# ```

# 

# Make sure PostgreSQL is running.

# 

# \### 3. Configure Environment Variables

# 

# Copy the example environment file:

# 

# ```powershell

# Copy-Item .env.example .env

# ```

# 

# Open `.env` and configure your PostgreSQL credentials:

# 

# ```properties

# SPRING\_DATASOURCE\_URL=jdbc:postgresql://localhost:5432/axlero\_dashboard

# SPRING\_DATASOURCE\_USERNAME=your\_postgres\_username

# SPRING\_DATASOURCE\_PASSWORD=your\_postgres\_password

# JWT\_SECRET=your\_secure\_random\_secret\_at\_least\_32\_bytes

# ```

# 

# Important:

# 

# \- Replace the username and password with your PostgreSQL credentials.

# \- Generate a secure random JWT secret of at least 32 bytes.

# \- Never commit your `.env` file or expose secret credentials.

# \- The application uses port `8080` for the backend by default.

# 

# \### 4. Run the Backend

# 

# From the project root directory:

# 

# ```powershell

# .\\mvnw.cmd spring-boot:run

# ```

# 

# Backend URL:

# 

# ```text

# http://localhost:8080

# ```

# 

# \### 5. Run the Frontend

# 

# Open another terminal:

# 

# ```powershell

# cd frontend

# npm install

# npm run dev

# ```

# 

# Open the local URL displayed by Vite. The default development URL is:

# 

# ```text

# http://localhost:5173

# ```

# 

# The Vite development server proxies `/api` and `/dashboard` requests to the backend at `http://127.0.0.1:8080` by default.

# 

# \## Authentication and Security

# 

# \- Users can register and log in using the authentication API.

# \- Passwords are encoded using BCrypt.

# \- JWT tokens are used for authenticated requests.

# \- The frontend stores the authentication token in browser local storage.

# \- Protected requests use the `Authorization: Bearer <token>` header.

# \- The backend uses stateless session management.

# \- CSRF protection is disabled for the stateless bearer-token API.

# \- CORS origins can be configured using `APP\_CORS\_ALLOWED\_ORIGINS`.

# \- JWT expiration is configured for 24 hours.

# 

# \## API Endpoints

# 

# \### Authentication APIs

# 

# | Method | Endpoint | Description |

# |---|---|---|

# | POST | `/api/auth/register` | Register a new user |

# | POST | `/api/auth/login` | User login |

# | GET | `/api/me` | Get authenticated user information |

# 

# \### Workspace APIs

# 

# | Method | Endpoint | Description |

# |---|---|---|

# | POST | `/api/workspaces` | Create a workspace |

# | GET | `/api/workspaces` | Get workspaces |

# 

# \### Dashboard APIs

# 

# | Method | Endpoint | Description |

# |---|---|---|

# | GET | `/api/dashboard` | Get dashboard details |

# | GET | `/dashboard` | Get dashboard summary |

# | GET | `/api/dashboard/widgets` | Get dashboard widgets |

# | POST | `/api/dashboard/widgets` | Create a widget |

# | PUT | `/api/dashboard/widgets` | Update widget configuration |

# | PUT | `/api/dashboard/widgets/order` | Update widget order |

# | DELETE | `/api/dashboard/widgets/{widgetId}` | Delete a widget |

# | GET | `/api/dashboard/widgets/{widgetId}/data` | Get widget chart data |

# 

# \### Dataset APIs

# 

# | Method | Endpoint | Description |

# |---|---|---|

# | GET | `/api/dashboard/dataset` | Get dashboard dataset |

# | POST | `/api/dashboard/dataset` | Upload dashboard dataset |

# | DELETE | `/api/dashboard/dataset` | Delete dashboard dataset |

# | POST | `/api/datasets/upload` | Upload dataset for legacy analytics |

# | GET | `/api/datasets/latest` | Get latest dataset |

# | GET | `/api/datasets/latest/analytics` | Get dataset analytics |

# 

# \### Export APIs

# 

# | Method | Endpoint | Description |

# |---|---|---|

# | GET | `/api/dashboard/export.csv` | Export widget chart results as CSV |

# | GET | `/api/dashboard/export.xlsx` | Export widget chart results as XLSX |

# | GET | `/dashboard/export/csv` | Export dashboard summary as CSV |

# | GET | `/dashboard/export/excel` | Export dashboard summary as Excel |

# 

# \*\*Note:\*\* The dashboard widget API and legacy CSV analytics API are separate compatibility surfaces with separate dataset records. The current dashboard UI uses the widget-chart API. Workspace management and legacy analytics are available through backend APIs.

# 

# \## Testing and Build

# 

# \### Backend Tests

# 

# Run from the project root:

# 

# ```powershell

# .\\mvnw.cmd test

# ```

# 

# Build the backend:

# 

# ```powershell

# .\\mvnw.cmd clean package

# ```

# 

# \### Frontend Linting

# 

# Run from the frontend directory:

# 

# ```powershell

# npm run lint

# ```

# 

# \### Frontend Production Build

# 

# ```powershell

# npm run build

# ```

# 

# The production build is generated inside:

# 

# ```text

# frontend/dist/

# ```

# 

# \## Project Structure

# 

# ```text

# axlero-dynamic-dashboard/

# │

# ├── frontend/

# │   ├── src/

# │   │   ├── services/

# │   │   ├── components/

# │   │   └── ...

# │   ├── package.json

# │   └── vite.config.js

# │

# ├── src/

# │   ├── main/

# │   │   ├── java/

# │   │   │   └── com/axlero/dashboard/

# │   │   │       ├── controller/

# │   │   │       ├── security/

# │   │   │       ├── service/

# │   │   │       └── repository/

# │   │   │

# │   │   └── resources/

# │   │       └── application.properties

# │   │

# │   └── test/

# │

# ├── .env.example

# ├── mvnw

# ├── mvnw.cmd

# ├── pom.xml

# └── README.md

# ```

# 

# \## Production Considerations

# 

# \- Use a secure PostgreSQL database.

# \- Configure strong JWT secrets.

# \- Use HTTPS for deployed applications.

# \- Configure allowed CORS origins.

# \- Keep credentials outside source control.

# \- Maintain regular database backups.

# 

# \## License

# 

# No license has been specified for this repository.



