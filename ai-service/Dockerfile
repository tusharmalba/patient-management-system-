# AI Service Dockerfile
FROM python:3.11-slim

WORKDIR /app

# Install dependencies first (Docker layer caching)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code
COPY . .

EXPOSE 8000

# Uvicorn: ASGI server for FastAPI
# --host 0.0.0.0  → Listen on all interfaces (needed in containers)
# --port 8000     → Port to listen on
# --workers 2     → Multiple worker processes for production
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--workers", "2"]
