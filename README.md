# Dissanayake-Super-Web-POS-BackEnd

## Run on any computer

### Requirements

- Java 21
- Internet access to the configured PostgreSQL database, or your own PostgreSQL instance

### Start with Maven Wrapper

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
./mvnw spring-boot:run
```

The backend runs on port `8080` by default. Override it with `SERVER_PORT`.

### Useful environment variables

```powershell
$env:SERVER_PORT="8080"
$env:DB_URL="jdbc:postgresql://<host>:5432/<database>?sslmode=require"
$env:DB_USERNAME="<db-user>"
$env:DB_PASSWORD="<db-password>"
$env:FRONTEND_BASE_URL="http://<frontend-host>:5173"
$env:REORDER_ACCEPT_URL_BASE="http://<backend-host>:8080/api/v1/reorder/accept"
$env:CORS_ALLOWED_ORIGIN_PATTERNS="http://localhost:5173,http://localhost:3000,http://192.168.*:5173,http://192.168.*:3000"
```

Mail is optional. The backend can start without it:

```powershell
$env:MAIL_USERNAME="<gmail-address>"
$env:MAIL_APP_PASSWORD="<gmail-app-password>"
$env:ADMIN_EMAIL="<admin-email>"
```

### Same-network access

1. Find the IP address of the computer running the backend.
2. Start the backend.
3. Open the frontend against `http://<that-ip>:8080`.
4. If the frontend is on another device, set `FRONTEND_BASE_URL` and `CORS_ALLOWED_ORIGIN_PATTERNS` to match that device URL.
