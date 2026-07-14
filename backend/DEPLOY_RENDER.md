Render deployment steps (Docker)

1. In Render dashboard, create a new Web Service -> "Docker" or "Connect a repo" and select this repository.
2. Set the root directory to `backend` (Render will use the Dockerfile in that folder).
3. Environment variables to add:
   - `SPRING_DATASOURCE_URL` = jdbc:postgresql://<HOST>:<PORT>/<DB_NAME>?sslmode=require
   - `DB_USERNAME` = <db user>
   - `DB_PASSWORD` = <db pass>
   - `FRONTEND_URL` = https://oop-autograder.vercel.app
   - `JWT_SECRET` = <your-jwt-secret>
   - `GOOGLE_CLIENT_ID` = <google client id>
4. (Optional) Add `JAVA_OPTS` if you need memory tuning, e.g. `-Xmx512m`.
5. Deploy. Check logs for successful startup and startup.

Local build & test:
```
cd backend
docker build -t eiu-backend:latest .
docker run -e DB_USERNAME=<user> -e DB_PASSWORD=<pass> -e SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/<db>?sslmode=require" -e PORT=8002 -p 8002:8002 eiu-backend:latest
```

