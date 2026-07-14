# EIU Capstone Project

This repository contains a full-stack web application for the EIU Capstone project. It combines a React + Vite frontend with a Java Spring Boot backend, Google authentication, JWT-based authorization, and a PostgreSQL database with Liquibase migrations.

## 1. What this project does

The application includes:
- A React-based web UI for login, dashboards, and course-related screens
- A Spring Boot backend for authentication and REST APIs
- Google OAuth sign-in flow with domain validation for `@eiu.edu.vn`
- JWT-based session handling for authenticated requests
- Swagger/OpenAPI documentation for backend APIs

## 2. Technology stack

### Frontend
- React 18
- Vite 7
- React Router DOM
- Tailwind CSS
- PostCSS / Autoprefixer
- npm

### Backend
- Java 17
- Spring Boot 3.2.2
- Spring Web
- Spring Security
- Spring Data JPA
- PostgreSQL
- Liquibase
- JWT (jjwt)
- Springdoc OpenAPI / Swagger UI

### Project orchestration
- Root-level npm scripts using `concurrently`

## 3. Prerequisites

Make sure these are installed before starting the project:
- Node.js 18+ and npm
- Java JDK 17
- Apache Maven
- PostgreSQL running locally
- A Google OAuth client ID for the authentication flow

## 4. Project structure

```text
EIU-Capstone/
├── package.json                 # Root scripts to run frontend + backend together
├── README.md                    # Project documentation
├── frontend/                    # React + Vite client
│   ├── package.json             # Frontend dependencies and scripts
│   ├── index.html               # Vite entry HTML
│   ├── vite.config.js          # Vite configuration (port 5173)
│   ├── src/                     # Frontend source code
│   │   ├── App.jsx              # Main app component
│   │   ├── main.jsx             # React bootstrap
│   │   ├── components/          # Reusable UI components
│   │   ├── context/             # React context providers
│   │   └── pages/               # Login / dashboard screens
│   └── tailwind.config.js      # Tailwind configuration
└── backend/                     # Spring Boot server
    ├── pom.xml                  # Maven project and dependencies
    ├── mvnw / mvnw.cmd          # Maven wrapper for Linux/Windows
    ├── src/main/java/           # Backend Java source code
    │   ├── controller/          # REST controllers
    │   ├── service/             # Business logic
    │   ├── repository/          # Data access layer
    │   ├── model/               # JPA entities and DTOs
    │   ├── config/              # Security / CORS / Swagger configs
    │   └── exception/           # Custom exception handling
    ├── src/main/resources/     # application.yml, application.properties, DB migration files
    │   ├── application.yml
    │   ├── application.properties
    │   └── db/changelog/        # Liquibase SQL migrations
    └── .env.example             # Example environment variables for local development
```

## 5. Environment configuration

### Backend environment variables
Create a local backend environment file from the example file.

From the project root in PowerShell:

```powershell
Copy-Item .\backend\.env.example .\backend\.env
```

Then edit the file at `backend/.env` and set values such as:

```env
GOOGLE_CLIENT_ID=your_google_client_id
JWT_SECRET=your_long_random_secret
FRONTEND_URL=http://localhost:5173
DB_USERNAME=postgres
DB_PASSWORD=your_postgres_password
```

### Database requirement
The backend is configured to connect to PostgreSQL at:
- Host: `localhost`
- Port: `5432`
- Database: `oop_autograder`

Make sure PostgreSQL is running and the database exists before starting the backend.

## 6. How to run the project

### Option A: Run both frontend and backend together
From the repository root:

```bash
npm install
npm start
```

This starts:
- Frontend at `http://localhost:5173/`
- Backend at `http://localhost:8002/`

### Option B: Run frontend and backend separately

#### Start the frontend only
```bash
npm install
npm run frontend
```

#### Start the backend only
```bash
cd backend
mvn spring-boot:run
```

## 7. Ports and URLs

- Frontend dev server: `http://localhost:5173/`
- Backend API base URL: `http://localhost:8002/`
- Swagger UI: `http://localhost:8002/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8002/v3/api-docs`

## 8. API overview

The backend exposes REST endpoints under the following base paths:
- Authentication: `/api/auth`
- Courses: `/api/courses`

### Example endpoints
- `POST /api/auth/google` — verifies a Google ID token and returns a JWT
- `GET /api/courses` — retrieves all courses
- `GET /api/courses/{id}` — retrieves a single course
- `POST /api/courses` — creates a new course

## 9. Swagger / OpenAPI

Swagger UI is enabled through Springdoc OpenAPI and is available at:

```text
http://localhost:8002/swagger-ui/index.html
```

Use Swagger UI to explore and test the CRUD-style endpoints exposed by the backend.

## 10. Authentication flow

1. The frontend sends a Google ID token to the backend.
2. The backend validates the token with Google.
3. The backend checks that the email is verified and belongs to the allowed domain `eiu.edu.vn`.
4. If valid, the backend returns a signed JWT that the frontend can use for authenticated requests.

## 11. Notes for development

- If you change backend configuration, update `backend/src/main/resources/application.yml` or `backend/.env`.
- Liquibase migrations are stored in `backend/src/main/resources/db/changelog/`.
- To stop the running services, press `Ctrl + C` in the terminal.
