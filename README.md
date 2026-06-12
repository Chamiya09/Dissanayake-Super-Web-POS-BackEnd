# Dissanayaka Super Web POS - Backend

Spring Boot backend API for the Dissanayaka Super Web POS system. It handles authentication, users, products, inventory, sales, supplier workflows, reorder requests, dashboards, mailbox integration, PDF invoices, CSV exports, and database migrations.

## Tech Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT authentication
- Spring Mail
- OpenPDF
- Maven Wrapper

## Main Features

- JWT login and authenticated API access.
- Product CRUD, pagination, search, bulk import, and supplier assignment.
- Inventory tracking, stock updates, stock adjustments, analytics, and inventory logs.
- POS sales recording with automatic inventory deduction.
- Sale updates, returns, invoices, and CSV export.
- Supplier management and purchase reorder workflows.
- Low-stock reorder support with email links for supplier acceptance.
- Owner and manager dashboard APIs.
- Gmail mailbox integration for inbox, sent mail, and outgoing purchase-order messages.

## Prerequisites

- Java 21
- PostgreSQL database
- Internet access if using a hosted PostgreSQL service
- PowerShell on Windows, or a Unix shell on macOS/Linux

## Quick Start

### Windows PowerShell

```powershell
cd "D:\Project\GitHub Project\Dissanayaka Super Web POS\Dissanayake-Super-Web-POS-BackEnd"
Copy-Item .env.example .env
.\mvnw.cmd spring-boot:run
```

### macOS / Linux

```bash
cd Dissanayake-Super-Web-POS-BackEnd
cp .env.example .env
./mvnw spring-boot:run
```

The backend starts on:

```text
http://localhost:8080
```

## Environment Variables

Set these values in your terminal, deployment platform, or `.env` loader if your environment supports one.

```env
SERVER_PORT=8080
DB_URL=jdbc:postgresql://localhost:5432/dissanayaka_pos
DB_USERNAME=postgres
DB_PASSWORD=your_password
FRONTEND_BASE_URL=http://localhost:5173
CORS_ALLOWED_ORIGIN_PATTERNS=http://localhost:5173,http://localhost:3000
REORDER_ACCEPT_URL_BASE=http://localhost:8080/api/v1/reorder/accept
```

Optional mail settings:

```env
MAIL_USERNAME=your_gmail_address
MAIL_APP_PASSWORD=your_gmail_app_password
MAIL_IMAP_HOST=imap.gmail.com
MAIL_IMAP_PORT=993
ADMIN_EMAIL=admin@example.com
```

Flyway can use the main database credentials by default. Override only if needed:

```env
FLYWAY_URL=jdbc:postgresql://localhost:5432/dissanayaka_pos
FLYWAY_USER=postgres
FLYWAY_PASSWORD=your_password
```

Security note: do not commit real database passwords, Gmail app passwords, or production JWT secrets. Override secrets through environment variables in production.

## Database Setup

1. Create a PostgreSQL database.
2. Set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
3. Start the backend.
4. Flyway runs migrations from:

```text
src/main/resources/db/migration
```

Hibernate is configured with:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

That means the schema must match the Flyway migrations.

## Available Commands

Run the API:

```powershell
.\mvnw.cmd spring-boot:run
```

Build the project:

```powershell
.\mvnw.cmd clean package
```

Run tests:

```powershell
.\mvnw.cmd test
```

Run the packaged JAR after building:

```powershell
java -jar target\web-pos-backend-0.0.1-SNAPSHOT.jar
```

Use `./mvnw` instead of `.\mvnw.cmd` on macOS/Linux.

## API Endpoint Groups

```text
POST   /api/auth/login

/api/users
/api/products
/api/inventory
/api/sales
/api/dashboard
/api/suppliers
/api/v1/reorder
/api/v1/mailbox

GET    /api/export-sales
```

Common examples:

```text
GET  /api/products
GET  /api/products/page?page=0&limit=10&search=rice
GET  /api/inventory/status
GET  /api/inventory/analytics
POST /api/sales
GET  /api/sales/{id}/invoice
GET  /api/dashboard/owner-stats
GET  /api/dashboard/manager-stats
GET  /api/v1/reorder/low-stock
```

Most endpoints require a JWT token:

```http
Authorization: Bearer <token>
```

## Project Structure

```text
src/main/java/com/dissayakesuper/web_pos_backend/
  auth/          Login and JWT authentication
  dashboard/     Owner and manager dashboard APIs
  inventory/     Stock, inventory status, logs, and import
  mailbox/       Gmail inbox, sent, and send APIs
  product/       Product CRUD, search, import, assignment
  reorder/       Reorder and purchase-order workflows
  sale/          POS sales, invoices, returns, export
  supplier/      Supplier CRUD and product assignment
  user/          User profile and management

src/main/resources/
  application.properties
  db/migration/
```

## Frontend Integration

For local development:

- Backend: `http://localhost:8080`
- Frontend: `http://localhost:5173`

The frontend Vite dev server proxies `/api` requests to the backend. If another device accesses the frontend, update:

```env
FRONTEND_BASE_URL=http://your-frontend-host:5173
CORS_ALLOWED_ORIGIN_PATTERNS=http://your-frontend-host:5173
```

## Troubleshooting

### Database connection fails

Check `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, network access, and PostgreSQL SSL settings.

### Flyway validation or migration error

Confirm the database schema matches the migrations in `src/main/resources/db/migration`. If a migration was edited after being applied, repair the Flyway history in the target database before enabling strict validation.

### Frontend CORS error

Add the frontend origin to:

```env
CORS_ALLOWED_ORIGIN_PATTERNS
```

### Mail does not send

Use a Gmail App Password, not your normal Gmail login password. Confirm SMTP access and `MAIL_USERNAME` / `MAIL_APP_PASSWORD`.
