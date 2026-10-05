# Contact Management System

A full-stack, role-based Contact Management System built using a Spring Boot microservices architecture and a React + TypeScript frontend.

The application provides centralized contact management, dashboard analytics, contact history, file handling, notifications, Excel import/export workflows, master-data management, user management, JWT authentication, and role-based authorization.

## Project Status

**Week 8: Final deployment, testing, documentation, and demo stage.**

The backend services are deployed across Railway and Render, while the frontend is deployed on Vercel.

## Live Application

| Component | Deployment |
|---|---|
| Frontend | https://contact-management-project-phi.vercel.app |
| API Gateway | https://gateway-0b1y.onrender.com |
| Contact Management | https://contact-management-mb84.onrender.com |
| Master Service | https://master-nzu2.onrender.com |
| User Service | https://user-gbq5.onrender.com |
| Auth Service | https://auth-production-bd55.up.railway.app |
| Config Server | https://config-server-production-e0e6.up.railway.app |
| Eureka Server | https://eureka-server-4933.up.railway.app |

> Production credentials, database passwords, JWT secrets, mail passwords, and API keys are intentionally not included in this repository documentation.

## Architecture

```text
                 +----------------------+
                 |   React + TypeScript |
                 |       Vercel         |
                 +----------+-----------+
                            |
                            v
                 +----------------------+
                 |     API Gateway      |
                 |       Render         |
                 +----+----+----+-------+
                      |    |    |
          +-----------+    |    +----------------+
          v                v                     v
 +----------------+ +--------------+ +----------------------+
 |  Auth Service  | | User Service | | Contact Management   |
 |    Railway     | |    Render    | |       Render         |
 +----------------+ +--------------+ +----------------------+
                                            |
                                            v
                                   +------------------+
                                   |  Master Service  |
                                   |      Render      |
                                   +------------------+

          +----------------------+     +------------------+
          |    Config Server     |     |  Eureka Server   |
          |       Railway        |     |     Railway      |
          +----------------------+     +------------------+

          +----------------------+     +------------------+
          |        MySQL         |     |      Redis       |
          |       Railway        |     |     Railway      |
          +----------------------+     +------------------+
```

## Microservices

| Service | Default Port | Responsibility |
|---|---:|---|
| Eureka Server | 8761 | Service discovery |
| Config Server | 8888 | Centralized configuration |
| Auth Service | 8081 | Authentication and JWT issuance |
| Master Service | 8083 | Master/dropdown data |
| User Service | 8084 | User and role-related operations |
| Contact Management | 8080 | Contacts, analytics, history, files, notifications and Excel workflows |
| Gateway | 8085 | Single API entry point, routing, CORS and security |
| Frontend | 5173 | React/Vite user interface |

Docker Compose maps the backend services to host ports `18xxx` to avoid conflicts during local development.

## Technology Stack

### Backend

- Java 21
- Spring Boot 4.1.0
- Spring Cloud 2025.1.2
- Spring Cloud Gateway
- Spring Cloud Config
- Netflix Eureka
- Spring Security
- JWT / OAuth2 Resource Server
- Spring Data JPA
- OpenFeign
- MySQL 8.4
- Redis 7.4
- Apache POI
- Gradle
- Docker

### Frontend

- React 19
- TypeScript
- Vite
- Material UI
- Redux Toolkit
- Axios
- React Hook Form
- Yup
- Material React Table / TanStack Table

### Deployment

- Railway: Auth, Config Server, Eureka Server, MySQL, Redis
- Render: User, Master, Contact Management, Gateway
- Vercel: React frontend

## Main Features

- JWT-based login and authorization
- Role-based access control
- Contact create/update workflow
- Contact search, filtering and pagination
- Dashboard analytics and status counts
- Dynamic table-header configuration
- Contact history loaded on demand by contact ID
- Contact file upload, download and delete
- Notification list, read status and attachments
- Excel validation and import
- Excel export/download approval workflow
- Downloadable Excel format/template
- User management
- Master/dropdown data management
- Soft-delete/inactive contact handling
- Centralized configuration using Spring Cloud Config
- API Gateway routing and CORS management

## Roles

The application supports the following roles:

- `ADMIN`
- `HOD`
- `MANAGEMENT`
- `USER`

### Contact Permissions

| Operation | ADMIN | HOD | MANAGEMENT | USER |
|---|:---:|:---:|:---:|:---:|
| View/search contacts | Yes | Yes | Yes | Yes |
| View contact history | Yes | Yes | Yes | Yes |
| View analytics | Yes | Yes | Yes | Yes |
| Create/update contact | Yes | No | Yes | No |
| Upload contact files | Yes | No | Yes | No |
| Download contact files | Yes | Yes | Yes | Yes |
| Delete contact files | Yes | No | Yes | No |
| Deactivate contact | Yes | No | No | No |

Excel operations have additional role-specific permissions enforced by the backend.

## Important API Endpoints

All frontend requests should normally go through the API Gateway.

### Authentication

```text
POST /auth/login
```

### Contacts

```text
POST   /contact/save
GET    /contact/fetch
GET    /contact/filter
GET    /contact/{id}
DELETE /contact/{id}
GET    /contact/history/{contactId}
GET    /contact/history-config
GET    /contact/status-count
GET    /contact/analytics
GET    /contact/name-suggestions
```

### Professional Table Header APIs

```text
GET /contact/table-headers/dashboard
GET /contact/table-headers/{table}
```

Example:

```text
GET /contact/table-headers/CONTACT
```

### Contact Files

```text
POST   /contact/file/upload
GET    /contact/file/{uuid}
DELETE /contact/file/{uuid}
```

### Notifications

```text
GET    /contact/notifications
PATCH  /contact/notifications/{id}/read
DELETE /contact/notifications/{id}
GET    /contact/notifications/{id}/attachment
```

### Excel

```text
POST /contact/excel/validate
POST /contact/excel/import
POST /contact/excel/export
GET  /contact/excel/download
POST /contact/excel/download-request
GET  /contact/excel/download-request/pending
POST /contact/excel/download-request/{id}/approve
POST /contact/excel/download-request/{id}/reject
GET  /contact/excel/format
```

### Master Data

```text
GET    /master/dropdown
GET    /master/{type}
POST   /master/{type}
PUT    /master/{type}/{id}
DELETE /master/{type}/{id}
```

### Users

```text
GET    /users
GET    /users/names
GET    /users/{id}
POST   /users
PUT    /users/{id}
DELETE /users/{id}
```

## Frontend API Behavior and Optimizations

The frontend uses a shared Axios client that automatically attaches the JWT access token from `localStorage`.

Current request optimizations include:

- Dashboard analytics reused instead of issuing redundant count requests where possible.
- Dashboard table-header configuration is cached/reused.
- Master dropdown data is cached to reduce repeated requests.
- Duplicate simultaneous filter requests are deduplicated.
- Contact history is not loaded globally; it is requested only when a specific contact history view is opened.
- Re-renders and tab switches are prevented from causing unnecessary duplicate history requests.
- Opening the notification UI does not itself trigger a read call; the read endpoint is used when the notification is actually marked/read.
- Blank/null dashboard values are presented as `N/A` where applicable.
- Unnecessary database flush operations were reduced in contact/file workflows to improve response time.

> Render free instances can enter a sleep/cold-start state. The first request after inactivity can therefore be slower than subsequent requests even when application code is optimized.

## Audit Fields

For newly created contacts:

- `Created At` stores the creation timestamp.
- Application/database timezone handling is aligned with `Asia/Kolkata` where configured.
- `Updated At` should remain `N/A` until an actual update occurs.
- `Updated By` should remain `N/A` until an actual update occurs.

After an update, the update timestamp and updater information are populated.

## Local Setup

### Prerequisites

Install:

- Java 21
- Docker Desktop / Docker Engine
- Node.js and npm
- Git

### 1. Clone the repository

```bash
git clone <repository-url>
cd Contact_Management_Project
```

### 2. Start backend infrastructure and services

From the project root:

```bash
docker compose up -d --build
```

Local host mappings from `docker-compose.yml`:

```text
MySQL             13306 -> 3306
Redis             16379 -> 6379
Config Server     18888 -> 8888
Eureka Server     18761 -> 8761
Auth              18081 -> 8081
Master            18083 -> 8083
User              18084 -> 8084
Contact Management 18080 -> 8080
Gateway            18085 -> 8085
```

To stop the local stack:

```bash
docker compose down
```

### 3. Run the frontend

```bash
cd contact-management/frontend
npm install
npm run dev
```

Frontend development server:

```text
http://localhost:5173
```

The current production frontend Axios configuration targets the deployed Gateway. For a fully local API test, configure the frontend API base URL for the local Gateway (`http://localhost:18085`) before running the test.

## Build Commands

Each Spring Boot service contains its own Gradle wrapper.

Example:

```bash
cd contact-management
./gradlew clean bootJar --no-daemon
```

Windows PowerShell/CMD:

```powershell
cd contact-management
.\gradlew.bat clean bootJar --no-daemon
```

Frontend production build:

```bash
cd contact-management/frontend
npm install
npm run build
```

## Docker

Backend services use Dockerfiles and can be built individually or through Docker Compose.

```bash
docker compose build
docker compose up -d
```

Local Docker is only required for local development/testing. The deployed application does not depend on Docker running on a developer machine.

## Deployment Notes

The repository is a monorepo. Backend services on Render are configured for manual deployment to prevent frontend-only Git pushes from unnecessarily redeploying all backend services.

Recommended deployment flow:

```text
Frontend-only change
  -> git push
  -> Vercel auto-deploy

Contact Management backend change
  -> git push
  -> Render Contact Management: Manual Deploy -> Deploy latest commit

Gateway change
  -> git push
  -> Render Gateway: Manual Deploy -> Deploy latest commit
```

Deploy additional services only when their own code/configuration has changed.

## Final Testing Checklist

Before demonstration/submission, verify the following end-to-end through the deployed frontend.

- [ ] Login succeeds and logout clears authentication.
- [ ] Dashboard analytics and tables load correctly.
- [ ] Dashboard table headers load through `/contact/table-headers/dashboard`.
- [ ] Blank/null values display as `N/A` where expected.
- [ ] Contact list/search/filter/pagination works.
- [ ] ADMIN can create/update contacts.
- [ ] MANAGEMENT can create/update contacts.
- [ ] New contact shows correct `Created At` time.
- [ ] New contact shows `Updated At = N/A` and `Updated By = N/A` before first update.
- [ ] Actual contact update populates `Updated At` and `Updated By`.
- [ ] Contact history loads only when a specific contact history is opened.
- [ ] Contact history request does not fire twice for the same action.
- [ ] File upload works for authorized roles.
- [ ] File download works.
- [ ] File delete works for authorized roles.
- [ ] Notification list/read behavior works without unnecessary duplicate calls.
- [ ] Excel format download works without HTTP 500.
- [ ] Excel validation/import works for permitted roles.
- [ ] Excel download/approval flow works.
- [ ] Master dropdown loads successfully.
- [ ] Role permissions behave correctly for ADMIN/HOD/MANAGEMENT/USER.
- [ ] Browser Network tab has no unexpected `401`, `403`, `404`, `500`, or `503` responses.
- [ ] No duplicate API calls are observed for filters, history, or cached table/dropdown configuration.
- [ ] Mobile/desktop layout is usable for the final demo.

## Suggested Final Demo Flow

1. Open the deployed frontend.
2. Login with an authorized demo account.
3. Show Dashboard analytics and tables.
4. Open Contact Management and demonstrate search/filtering.
5. Create a contact.
6. Verify audit values for a newly created contact.
7. Update the contact and verify update audit values.
8. Open that contact's history.
9. Demonstrate file upload/download if required.
10. Demonstrate notifications.
11. Demonstrate Excel template/import/download workflow.
12. Briefly show role-based behavior.
13. Show the deployed microservices/API architecture.

## Security Notes

- Do not commit production passwords, JWT secrets, mail credentials, API keys, or database credentials.
- Use deployment environment variables for secrets.
- Rotate any credentials that were exposed during development/testing before production use.
- Use HTTPS for deployed services.
- Keep role authorization enforced on the backend; frontend visibility alone is not a security boundary.

## Repository Structure

```text
Contact_Management_Project/
├── auth/
├── common/
├── config-server/
├── contact-management/
│   ├── frontend/
│   └── src/
├── eureka-server/
├── gateway/
├── master/
├── user/
├── docker-compose.yml
└── README.md
```

## Final Submission

The project is ready for final Week 8 completion after the final deployed regression test passes. The remaining submission activity is to demonstrate the live application and submit the repository/project according to the required format.
