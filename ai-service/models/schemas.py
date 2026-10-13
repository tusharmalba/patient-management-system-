"""
╔══════════════════════════════════════════════════════════════════════╗
║                     PYDANTIC MODELS                                  ║
║                                                                       ║
║  Pydantic = Python's data validation library                          ║
║  FastAPI uses Pydantic for:                                           ║
║    - Request body validation (like @Valid in Spring)                  ║
║    - Automatic JSON serialization/deserialization                     ║
║    - Auto-generated OpenAPI schema (Swagger docs)                     ║
║                                                                       ║
║  BaseModel → All request/response classes extend this                 ║
║  Field(...) → Required field (like @NotNull)                          ║
║  Field(default) → Optional field with default value                  ║
╚══════════════════════════════════════════════════════════════════════╝
"""

from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from enum import Enum


class RiskLevel(str, Enum):
    LOW    = "LOW"
    MEDIUM = "MEDIUM"
    HIGH   = "HIGH"


# ─────────────────────────────────────────────
# SYMPTOM CHECKER MODELS
# ─────────────────────────────────────────────

class SymptomCheckRequest(BaseModel):
    """Input: list of symptoms the patient is experiencing."""
    symptoms: List[str] = Field(..., min_items=1,
        description="List of symptoms e.g. ['fever', 'cough', 'fatigue']",
        example=["fever", "cough", "difficulty breathing", "chest pain"])
    patient_age: Optional[int] = Field(None, ge=0, le=120, description="Patient age in years")
    patient_gender: Optional[str] = Field(None, description="MALE/FEMALE/OTHER")
    duration_days: Optional[int] = Field(None, ge=0, description="How long symptoms have been present")


class PossibleDisease(BaseModel):
    disease: str
    confidence: float      # 0.0 to 1.0
    severity: str          # mild / moderate / severe
    description: str
    recommended_specialist: str


class SymptomCheckResponse(BaseModel):
    possible_diseases: List[PossibleDisease]
    urgency_level: str          # routine / urgent / emergency
    seek_immediate_care: bool
    general_advice: str
    disclaimer: str


# ─────────────────────────────────────────────
# DISEASE / RISK PREDICTION MODELS
# ─────────────────────────────────────────────

class PatientRiskRequest(BaseModel):
    """
    Patient health factors for risk prediction.
    Maps directly to Patient entity fields sent from Spring Boot.
    """
    age: int = Field(..., ge=0, le=120)
    gender: Optional[str] = None
    has_diabetes: bool = False
    is_smoker: bool = False
    has_heart_disease: bool = False
    has_high_blood_pressure: bool = False
    bmi: Optional[float] = Field(None, ge=10, le=60)
    cholesterol_level: Optional[float] = None
    symptoms: Optional[List[str]] = None


class RiskPredictionResponse(BaseModel):
    risk_level: RiskLevel
    risk_score: int             # 0–10
    risk_factors: List[str]     # Which factors contributed
    protective_factors: List[str]
    recommendations: List[str]
    predicted_conditions: List[str]   # Conditions patient is at risk for
    lifestyle_changes: List[str]


# ─────────────────────────────────────────────
# REPORT SUMMARIZER MODELS
# ─────────────────────────────────────────────

class ReportSummaryRequest(BaseModel):
    """Medical report text to be summarized."""
    report_text: str = Field(..., min_length=10,
        description="Full text content of the medical report")
    report_type: Optional[str] = Field(None,
        description="Type: blood_test, x_ray, mri, prescription, discharge_summary")
    patient_age: Optional[int] = None
    patient_conditions: Optional[List[str]] = None


class ReportSummaryResponse(BaseModel):
    summary: str                     # Plain-language summary
    key_findings: List[str]          # Bullet points of important findings
    abnormal_values: List[str]       # Values outside normal range
    recommendations: List[str]       # Next steps
    urgency: str                     # routine / follow_up / urgent
    simplified_explanation: str      # Explain to a non-medical person


# ─────────────────────────────────────────────
# CHATBOT MODELS
# ─────────────────────────────────────────────

class ChatMessage(BaseModel):
    role: str    # "user" or "assistant"
    content: str


class ChatRequest(BaseModel):
    """
    Multi-turn chat request.
    conversation_history holds prior messages so AI has context.
    """
    message: str = Field(..., min_length=1, description="Current user message")
    conversation_history: Optional[List[ChatMessage]] = Field(
        default=[], description="Previous messages for context")
    patient_context: Optional[Dict[str, Any]] = Field(
        None, description="Optional patient data for personalized responses")


class ChatResponse(BaseModel):
    reply: str
    sources_consulted: Optional[List[str]] = None
    follow_up_questions: Optional[List[str]] = None    # Suggested follow-ups
    disclaimer: str = "This is for informational purposes only. Always consult a doctor."
