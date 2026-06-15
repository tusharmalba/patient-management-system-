# 🏥 Patient Management System (PMS)

> Production-ready full-stack healthcare application built with Spring Boot 3, React, MySQL, JWT, FastAPI AI Module, and Docker.

---

## 🏗️ Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT BROWSER                           │
│                    React 18 + Vite + Axios                      │
│              http://localhost (Docker) or :5173 (dev)           │
└─────────────────────────┬───────────────────────────────────────┘
                          │ HTTP (REST API)
                          │ Authorization: Bearer <JWT>
┌─────────────────────────▼───────────────────────────────────────┐
│                    SPRING BOOT 3 BACKEND                        │
│                       Port: 8080                                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────┐  │
│  │Controller│→ │ Service  │→ │Repository│→ │   MySQL 8     │  │
│  │  Layer   │  │  Layer   │  │  Layer   │  │   Port: 3306  │  │
│  └──────────┘  └──────────┘  └──────────┘  └───────────────┘  │
│                     │                                           │
│              ┌──────▼──────┐                                    │
│              │ JWT Filter  │  (validates every request)         │
│              └─────────────┘                                    │
└─────────────────────────┬───────────────────────────────────────┘
                          │ HTTP REST (internal)
┌─────────────────────────▼───────────────────────────────────────┐
│                  PYTHON FastAPI AI SERVICE                      │
│                       Port: 8000                                │
│  ┌─────────────────┐  ┌──────────────┐  ┌──────────────────┐  │
│  │ Symptom Checker │  │ Risk Predict │  │ Report Summarize │  │
│  └─────────────────┘  └──────────────┘  └──────────────────┘  │
│  ┌────────────────────────────────────────────────────────────┐ │
│  │              Healthcare Chatbot                             │ │
│  └────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

```bash
# Clone the project
git clone <your-repo-url>
cd pms

# Start everything with Docker Compose
docker-compose up --build

# Access:
# Frontend:  http://localhost
# API:       http://localhost:8080
# Swagger:   http://localhost:8080/swagger-ui.html
# AI Docs:   http://localhost:8000/docs

# Default login:
# Email:    admin@pms.com
# Password: Admin@123
```

---

## 📁 Project Structure

```
pms/
├── backend/                          # Spring Boot 3 (Java 17)
│   ├── src/main/java/com/pms/
│   │   ├── PatientManagementSystemApplication.java  # Entry point
│   │   ├── config/
│   │   │   ├── SecurityConfig.java   # JWT + CORS + Role config
│   │   │   ├── ApplicationConfig.java# UserDetailsService bean
│   │   │   ├── SwaggerConfig.java    # OpenAPI 3 documentation
│   │   │   ├── RestTemplateConfig.java# HTTP client for AI calls
│   │   │   └── DataInitializer.java  # Seeds admin on first run
│   │   ├── controller/
│   │   │   ├── AuthController.java   # POST /api/auth/{register,login}
│   │   │   ├── PatientController.java# CRUD + Search + Priority Queue
│   │   │   ├── DoctorController.java # CRUD + Availability
│   │   │   ├── AppointmentController.java # Book + Cancel + Status
│   │   │   ├── DashboardController.java   # Analytics
│   │   │   ├── MedicalReportController.java# File Upload/Download
│   │   │   └── AiController.java     # Gateway to Python AI service
│   │   ├── service/
│   │   │   ├── AuthService.java      # Register, Login, JWT generation
│   │   │   ├── PatientService.java   # Business logic + Risk Scoring
│   │   │   ├── DoctorService.java    # Doctor management
│   │   │   ├── AppointmentService.java# Conflict detection
│   │   │   ├── DashboardService.java # Aggregate analytics
│   │   │   ├── FileStorageService.java# Upload/serve files
│   │   │   └── AiServiceClient.java  # HTTP calls to FastAPI
│   │   ├── repository/               # Spring Data JPA interfaces
│   │   ├── entity/                   # Hibernate mapped classes
│   │   ├── dto/
│   │   │   ├── request/              # Input DTOs (with @Valid)
│   │   │   └── response/             # Output DTOs (safe subset)
│   │   ├── security/
│   │   │   ├── jwt/JwtUtil.java      # Token generate/validate
│   │   │   └── filter/JwtAuthFilter.java # Per-request JWT check
│   │   ├── exception/
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   ├── DuplicateResourceException.java
│   │   │   └── AppointmentConflictException.java
│   │   ├── enums/                    # Role, Priority, RiskLevel, etc.
│   │   └── audit/AuditService.java   # Write to AuditLog table
│   ├── src/main/resources/
│   │   ├── application.properties    # Dev config
│   │   └── application-prod.properties # Production config
│   ├── Dockerfile                    # Multi-stage build
│   └── pom.xml                       # Maven dependencies
│
├── frontend/                         # React 18 + Vite
│   ├── src/
│   │   ├── App.jsx                   # Routes + ProtectedRoute
│   │   ├── main.jsx                  # React entry point
│   │   ├── index.css                 # Global styles
│   │   ├── pages/
│   │   │   ├── LoginPage.jsx         # JWT login form
│   │   │   ├── RegisterPage.jsx      # Registration form
│   │   │   ├── DashboardPage.jsx     # Analytics + metrics
│   │   │   ├── PatientsPage.jsx      # Full CRUD + risk badges
│   │   │   ├── DoctorsPage.jsx       # Doctor management
│   │   │   └── AppointmentsPage.jsx  # Book + cancel + complete
│   │   ├── components/
│   │   │   └── Layout.jsx            # Sidebar + nav + Outlet
│   │   ├── services/
│   │   │   └── api.js                # Axios instance + all API calls
│   │   └── utils/
│   │       └── auth.js               # Token helpers, role checks
│   ├── Dockerfile                    # Node build + Nginx serve
│   ├── nginx.conf                    # React Router + API proxy
│   └── package.json
│
├── ai-service/                       # Python FastAPI
│   ├── main.py                       # FastAPI app + routes
│   ├── models/schemas.py             # Pydantic request/response
│   ├── routers/
│   │   ├── symptom_checker.py        # Rule-based + OpenAI
│   │   ├── disease_prediction.py     # Risk scoring algorithm
│   │   ├── report_summarizer.py      # Medical report NLP
│   │   └── chatbot.py                # Multi-turn chat
│   ├── requirements.txt
│   └── Dockerfile
│
├── docker-compose.yml                # Orchestrates all 4 services
├── DEPLOYMENT.md                     # Railway + Render guide
└── README.md                         # This file
```

---

## 🔒 JWT Authentication Flow

```
1. POST /api/auth/login  { email, password }
         │
         ▼
   AuthenticationManager.authenticate()
         │
         ▼
   DaoAuthenticationProvider
     → UserDetailsService.loadUserByUsername(email) → DB lookup
     → passwordEncoder.matches(raw, hash)           → BCrypt check
         │
         ▼ (success)
   JwtUtil.generateToken(user)
     → Creates: { sub: email, role: ADMIN, iat: now, exp: now+24h }
     → Signs with HMAC-SHA256 + secret key
         │
         ▼
   Response: { token: "eyJ...", role: "ADMIN", ... }

─────────────────────────────────────────────────────────

2. Every subsequent request:
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
         │
         ▼
   JwtAuthFilter (runs before every request)
     → Extract token from "Bearer " prefix
     → JwtUtil.extractUsername(token) → email
     → UserDetailsService.loadUserByUsername(email) → User
     → JwtUtil.isTokenValid(token, user)
     → SecurityContextHolder.setAuthentication(user)
         │
         ▼
   @PreAuthorize checks pass → Controller runs
```

---

## 📡 API Endpoints Summary

### Authentication (Public)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Create account |
| POST | `/api/auth/login` | Get JWT token |

### Patients (🔐 Auth Required)
| Method | Endpoint | Role | Description |
|--------|----------|------|-------------|
| POST | `/api/patients` | ADMIN/DOCTOR | Create patient |
| GET | `/api/patients` | ADMIN/DOCTOR | All patients (paginated) |
| GET | `/api/patients/{id}` | Any | Get by ID |
| GET | `/api/patients/search?name=john` | ADMIN/DOCTOR | Search by name |
| GET | `/api/patients/priority-queue` | ADMIN/DOCTOR | Emergency queue |
| GET | `/api/patients/high-risk` | ADMIN/DOCTOR | HIGH risk patients |
| PUT | `/api/patients/{id}` | ADMIN/DOCTOR | Update patient |
| DELETE | `/api/patients/{id}` | ADMIN | Soft delete |

### Doctors
| Method | Endpoint | Role |
|--------|----------|------|
| POST | `/api/doctors` | ADMIN |
| GET | `/api/doctors` | Any auth |
| GET | `/api/doctors/search?name=` | Any auth |
| GET | `/api/doctors/specialization?specialization=` | Any auth |
| PUT | `/api/doctors/{id}` | ADMIN/DOCTOR |
| DELETE | `/api/doctors/{id}` | ADMIN |

### Appointments
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/appointments` | Book (conflict check) |
| GET | `/api/appointments/today` | Today's schedule |
| GET | `/api/appointments/patient/{id}` | Patient's history |
| GET | `/api/appointments/doctor/{id}` | Doctor's schedule |
| PUT | `/api/appointments/{id}` | Reschedule |
| PATCH | `/api/appointments/{id}/cancel` | Cancel |
| PATCH | `/api/appointments/{id}/status` | Confirm/Complete |

### AI Features
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/ai/symptom-check` | Diagnose from symptoms |
| POST | `/api/ai/predict-risk` | Patient risk prediction |
| POST | `/api/ai/summarize-report` | Summarize medical report |
| POST | `/api/ai/chat` | Healthcare chatbot |

---

## 🧱 Key Design Patterns Used

| Pattern | Where Used | Purpose |
|---------|-----------|---------|
| **DTO Pattern** | All layers | Never expose entities directly |
| **Repository Pattern** | Data layer | Abstract DB operations |
| **Service Layer** | Business logic | Separate concerns |
| **Soft Delete** | All entities | `isDeleted` flag, never DELETE |
| **Audit Trail** | Key operations | `AuditLog` table tracks changes |
| **JWT Stateless Auth** | Security | No sessions, token per request |
| **Global Exception Handler** | Error handling | Consistent error responses |
| **Multi-stage Docker** | Deployment | Small production images |
| **Pagination** | List endpoints | Never load all records at once |
| **Conflict Detection** | Appointments | Interval overlap algorithm |

---

## 🛠️ Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend | Spring Boot | 3.2.0 |
| Language | Java | 17 (LTS) |
| ORM | Hibernate / Spring Data JPA | 6.x |
| Database | MySQL | 8.0 |
| Security | Spring Security + JWT (JJWT) | 0.11.5 |
| Documentation | SpringDoc OpenAPI (Swagger) | 2.2.0 |
| Code Gen | Lombok | Latest |
| Frontend | React | 18.2 |
| Build Tool | Vite | 5.0 |
| HTTP Client | Axios | 1.6 |
| Routing | React Router | 6.20 |
| AI Service | FastAPI + Python | 3.11 |
| AI/ML | OpenAI API (optional) | 1.3 |
| Container | Docker + Docker Compose | Latest |
| Web Server | Nginx | Alpine |

---

## 📄 License

MIT License — Free to use for learning and projects.
