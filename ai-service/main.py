"""
╔══════════════════════════════════════════════════════════════════════╗
║                    AI SERVICE — FastAPI                              ║
║                                                                       ║
║  Separate Python microservice for AI features:                        ║
║    1. Symptom Checker        → Possible diseases from symptoms        ║
║    2. Disease Prediction     → Risk prediction from patient data      ║
║    3. Medical Report Summary → PDF/text summarization                 ║
║    4. Healthcare Chatbot     → Conversational health Q&A              ║
║                                                                       ║
║  WHY A SEPARATE SERVICE?                                              ║
║    - Python has richer ML ecosystem (scikit-learn, transformers)      ║
║    - Spring Boot doesn't need Python dependencies                     ║
║    - Can scale AI service independently                               ║
║    - Can upgrade/replace AI without touching Java code                ║
║                                                                       ║
║  COMMUNICATION:                                                       ║
║    Spring Boot → HTTP REST → FastAPI                                  ║
║    Spring Boot calls: POST http://ai-service:8000/ai/symptom-check   ║
║                                                                       ║
║  FastAPI advantages over Flask:                                       ║
║    - Automatic OpenAPI/Swagger docs at /docs                          ║
║    - Type hints → automatic request validation (Pydantic)             ║
║    - Async support (better performance)                               ║
║    - Much faster than Flask (comparable to Node.js)                   ║
╚══════════════════════════════════════════════════════════════════════╝
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from routers import symptom_checker, disease_prediction, report_summarizer, chatbot

# Create FastAPI application
app = FastAPI(
    title="PMS AI Service",
    description="AI-powered healthcare assistant — Symptom Checker, Disease Prediction, Report Summarizer, Chatbot",
    version="1.0.0",
    docs_url="/docs",       # Swagger UI at /docs
    redoc_url="/redoc",     # ReDoc at /redoc
)

# ─────────────────────────────────────────────
# CORS — Allow Spring Boot to call this service
# ─────────────────────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080", "http://backend:8080", "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ─────────────────────────────────────────────
# REGISTER ROUTERS
# Each router handles a specific AI feature
# ─────────────────────────────────────────────
app.include_router(symptom_checker.router,    prefix="/ai", tags=["Symptom Checker"])
app.include_router(disease_prediction.router, prefix="/ai", tags=["Disease Prediction"])
app.include_router(report_summarizer.router,  prefix="/ai", tags=["Report Summarizer"])
app.include_router(chatbot.router,            prefix="/ai", tags=["Healthcare Chatbot"])


@app.get("/health", tags=["Health"])
def health_check():
    """Health check endpoint for Docker/deployment platforms."""
    return {"status": "healthy", "service": "PMS AI Service"}


@app.get("/", tags=["Info"])
def root():
    return {
        "message": "PMS AI Service is running",
        "docs": "/docs",
        "endpoints": {
            "symptom_checker":    "POST /ai/symptom-check",
            "disease_prediction": "POST /ai/predict-risk",
            "report_summary":     "POST /ai/summarize-report",
            "chatbot":            "POST /ai/chat",
        }
    }
