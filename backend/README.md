# Google OAuth Authentication Backend

This backend is a Spring Boot application that verifies Google ID tokens and accepts only `eiu.edu.vn` email addresses.

## Setup

1. Open the backend folder:
   ```bash
   cd backend
   ```
2. Make sure you have Java 17+ and Maven installed.
3. Copy `.env.example` to `.env` and set `GOOGLE_CLIENT_ID`, `JWT_SECRET`, and `FRONTEND_URL`.

## Run backend

```bash
mvn spring-boot:run
```

## API

- `POST /api/auth/google`
  - request body: `{ "token": "<id_token_from_google>" }`
  - response: `{ "accessToken": "<jwt>" }`

## Configuration

The backend uses `application.yml` in `src/main/resources` and can import values from a local `.env` file.

## Behavior

- Validates the Google token via `https://oauth2.googleapis.com/tokeninfo`
- Checks `email_verified` true and allowed domain `eiu.edu.vn`
- Issues a signed JWT with a 1-hour expiration when valid
