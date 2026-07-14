# EIU Capstone Project

## Frontend

1. From the repository root, install dependencies:
   ```bash
   npm install
   ```
2. Start the frontend development server:
   ```bash
   npm start
   ```

If your frontend uses Vite instead of Create React App, use:

```bash
npm run dev
```

The frontend source is located in `frontend/src`.

## Backend

The backend is implemented in Java Spring Boot.

1. Open the backend folder:
   ```bash
   cd backend
   ```
2. Run the backend:
   ```bash
   mvn spring-boot:run
   ```
3. Configure backend values in `backend/.env` or `backend/src/main/resources/application.yml`.

## Notes

- The frontend sends Google ID tokens to `http://localhost:8080/api/auth/google`.
- The backend verifies Google authentication and only accepts emails ending in `eiu.edu.vn`.
