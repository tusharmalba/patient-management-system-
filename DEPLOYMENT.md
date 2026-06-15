# 🚀 Deployment Guide — Railway & Render

## Table of Contents
1. [Local Development](#local-development)
2. [Railway Deployment](#railway-deployment)
3. [Render Deployment](#render-deployment)
4. [Environment Variables Reference](#environment-variables)
5. [Troubleshooting](#troubleshooting)

---

## Local Development

### Option A — Direct (fastest for development)

```bash
# 1. Start MySQL locally
mysql -u root -p
CREATE DATABASE pms_db;
EXIT;

# 2. Run Spring Boot backend
cd pms/backend
./mvnw spring-boot:run
# API running at: http://localhost:8080
# Swagger UI at:  http://localhost:8080/swagger-ui.html

# 3. Run React frontend (new terminal)
cd pms/frontend
npm install
npm run dev
# Frontend at: http://localhost:5173

# 4. Run AI Service (new terminal)
cd pms/ai-service
pip install -r requirements.txt
uvicorn main:app --reload --port 8000
# AI docs at: http://localhost:8000/docs
```

### Option B — Docker Compose (full stack)

```bash
cd pms

# Build and start all 4 services
docker-compose up --build

# Access:
# Frontend:   http://localhost
# Backend:    http://localhost:8080
# AI Service: http://localhost:8000
# MySQL:      localhost:3307

# Stop everything
docker-compose down

# Stop and delete all data (fresh start)
docker-compose down -v
```

### Default Login
```
Email:    admin@pms.com
Password: Admin@123
```

---

## Railway Deployment

Railway is ideal for full-stack apps with databases.
Free tier: $5 credit/month (enough for a demo project).

### Step 1 — Create Railway Account
1. Go to https://railway.app
2. Sign up with GitHub

### Step 2 — Deploy MySQL Database

```bash
# In Railway dashboard:
1. New Project → Add a service → Database → MySQL
2. Railway auto-creates the DB and gives you connection details
3. Copy: MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD, MYSQL_DATABASE
```

### Step 3 — Deploy Spring Boot Backend

```bash
# Option 1: Deploy from GitHub (recommended)
1. Push your code to GitHub
2. Railway → New Service → GitHub Repo → select your repo
3. Set Root Directory: backend
4. Railway auto-detects Dockerfile

# Option 2: Railway CLI
npm install -g @railway/cli
railway login
cd pms/backend
railway up
```

**Set these environment variables in Railway dashboard:**
```env
SPRING_DATASOURCE_URL=jdbc:mysql://${{MySQL.MYSQL_HOST}}:${{MySQL.MYSQL_PORT}}/${{MySQL.MYSQL_DATABASE}}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
SPRING_DATASOURCE_USERNAME=${{MySQL.MYSQL_USER}}
SPRING_DATASOURCE_PASSWORD=${{MySQL.MYSQL_PASSWORD}}
APP_JWT_SECRET=your_super_secret_jwt_key_minimum_32_characters_long
SPRING_PROFILES_ACTIVE=prod
APP_UPLOAD_DIR=/app/uploads/medical-reports
APP_AI_SERVICE_URL=https://your-ai-service.railway.app
```

### Step 4 — Deploy Python AI Service

```bash
# In Railway dashboard:
1. New Service → GitHub Repo → select repo
2. Set Root Directory: ai-service
3. Railway detects Dockerfile automatically

# Optional environment variable:
OPENAI_API_KEY=sk-your-key  # Only if you want GPT integration
```

### Step 5 — Deploy React Frontend

```bash
# In Railway dashboard:
1. New Service → GitHub Repo → select repo
2. Set Root Directory: frontend
3. Railway detects Dockerfile (Nginx)

# Set build argument:
VITE_API_URL=https://your-backend.railway.app
```

---

## Render Deployment

Render is another great option with a generous free tier.
Free tier: Services sleep after 15 min inactivity (spin up takes ~30s).

### Step 1 — Deploy MySQL on PlanetScale (recommended with Render)

Render's managed PostgreSQL is easier, but if you need MySQL:
1. Sign up at https://planetscale.com (free MySQL-compatible DB)
2. Create database → Get connection string

### Step 2 — Deploy Spring Boot Backend

1. Go to https://render.com → New → Web Service
2. Connect your GitHub repository
3. Configure:
   ```
   Name:         pms-backend
   Region:       Choose closest to you
   Branch:       main
   Root Dir:     backend
   Runtime:      Docker
   Dockerfile:   ./Dockerfile
   ```

4. Add environment variables:
   ```env
   SPRING_DATASOURCE_URL=jdbc:mysql://host:3306/pms_db?useSSL=true&serverTimezone=UTC
   SPRING_DATASOURCE_USERNAME=your_db_user
   SPRING_DATASOURCE_PASSWORD=your_db_password
   APP_JWT_SECRET=your_minimum_32_character_secret_key_here
   SPRING_PROFILES_ACTIVE=prod
   ```

### Step 3 — Deploy AI Service on Render

1. New → Web Service
2. Root Directory: `ai-service`
3. Runtime: Docker
4. Environment Variable: `OPENAI_API_KEY=sk-...` (optional)

### Step 4 — Deploy Frontend on Render

1. New → Static Site (for pure React, no Nginx needed)
   OR New → Web Service with Docker (uses our Nginx setup)
2. Root Directory: `frontend`
3. If Static Site:
   ```
   Build Command: npm install && npm run build
   Publish Dir:   dist
   ```
4. Set environment variable:
   ```env
   VITE_API_URL=https://pms-backend.onrender.com
   ```

---

## Environment Variables

### Backend (Spring Boot)
| Variable | Description | Example |
|----------|-------------|---------|
| `SPRING_DATASOURCE_URL` | MySQL JDBC URL | `jdbc:mysql://host:3306/db` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `pmsuser` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `secure_password` |
| `APP_JWT_SECRET` | JWT signing key (min 32 chars!) | `my_super_secret_key_32chars...` |
| `SPRING_PROFILES_ACTIVE` | Active profile | `prod` |
| `APP_UPLOAD_DIR` | File upload directory | `/app/uploads/medical-reports` |
| `APP_AI_SERVICE_URL` | Python AI service URL | `http://ai-service:8000` |

### AI Service (FastAPI)
| Variable | Description | Required |
|----------|-------------|----------|
| `OPENAI_API_KEY` | OpenAI API key | No (uses rule-based fallback) |

### Frontend (React/Vite)
| Variable | Description | Example |
|----------|-------------|---------|
| `VITE_API_URL` | Backend API base URL | `https://api.yourapp.com` |

---

## Troubleshooting

### Backend won't connect to MySQL
```bash
# Check MySQL is running
docker ps | grep mysql

# Check environment variables are set
docker exec pms-backend env | grep SPRING

# Check MySQL health
docker exec pms-mysql mysqladmin ping -u root -prootpassword
```

### JWT errors (401 Unauthorized)
```bash
# Check APP_JWT_SECRET is at least 32 characters
# Check token hasn't expired (default: 24 hours)
# Check Authorization header format: "Bearer <token>"
```

### Spring Boot won't start (port conflict)
```bash
# Check what's using port 8080
lsof -i :8080
# Kill it or change server.port in application.properties
```

### AI Service connection refused
```bash
# Check AI service is running
curl http://localhost:8000/health

# Check Docker network (services must be on same network)
docker network inspect pms_pms-network
```

### File upload failing
```bash
# Check upload directory exists and has write permissions
ls -la uploads/medical-reports/
chmod 755 uploads/medical-reports/

# Check file size limit (default 10MB in application.properties)
```

### Frontend blank page after deployment
```bash
# Check Nginx config for React Router support
# Must have: try_files $uri $uri/ /index.html;
# This handles client-side routing on page refresh
```
