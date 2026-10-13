"""
╔══════════════════════════════════════════════════════════════════════╗
║                    SYMPTOM CHECKER ROUTER                            ║
║                                                                       ║
║  Two-tier approach:                                                   ║
║  1. Rule-based engine (fast, free, works offline)                     ║
║     Maps symptom combinations → disease possibilities                 ║
║  2. OpenAI GPT fallback (richer reasoning, needs API key)             ║
║     Sends symptoms to GPT-4 with a medical system prompt              ║
║                                                                       ║
║  In production: use a trained ML classifier or medical knowledge base  ║
╚══════════════════════════════════════════════════════════════════════╝
"""

import os
import json
from fastapi import APIRouter
from models.schemas import SymptomCheckRequest, SymptomCheckResponse, PossibleDisease

router = APIRouter()

# ─────────────────────────────────────────────
# SYMPTOM → DISEASE KNOWLEDGE BASE
# In production: replace with a real medical ontology (ICD-10 codes)
# ─────────────────────────────────────────────
SYMPTOM_DB = {
    frozenset(["fever", "cough", "fatigue"]): {
        "disease": "Influenza (Flu)",
        "confidence": 0.80, "severity": "moderate",
        "description": "Viral respiratory infection with systemic symptoms",
        "specialist": "General Physician"
    },
    frozenset(["fever", "cough", "difficulty breathing", "chest pain"]): {
        "disease": "Pneumonia",
        "confidence": 0.75, "severity": "severe",
        "description": "Lung infection requiring prompt medical attention",
        "specialist": "Pulmonologist"
    },
    frozenset(["chest pain", "shortness of breath", "left arm pain"]): {
        "disease": "Myocardial Infarction (Heart Attack)",
        "confidence": 0.85, "severity": "severe",
        "description": "EMERGENCY: Possible heart attack. Call emergency services immediately!",
        "specialist": "Cardiologist (EMERGENCY)"
    },
    frozenset(["headache", "nausea", "sensitivity to light"]): {
        "disease": "Migraine",
        "confidence": 0.78, "severity": "moderate",
        "description": "Severe recurring headache disorder",
        "specialist": "Neurologist"
    },
    frozenset(["frequent urination", "excessive thirst", "fatigue"]): {
        "disease": "Diabetes Mellitus",
        "confidence": 0.72, "severity": "moderate",
        "description": "Blood glucose regulation disorder",
        "specialist": "Endocrinologist"
    },
    frozenset(["joint pain", "swelling", "morning stiffness"]): {
        "disease": "Rheumatoid Arthritis",
        "confidence": 0.70, "severity": "moderate",
        "description": "Autoimmune joint inflammation disorder",
        "specialist": "Rheumatologist"
    },
    frozenset(["abdominal pain", "diarrhea", "nausea", "vomiting"]): {
        "disease": "Gastroenteritis",
        "confidence": 0.82, "severity": "mild",
        "description": "Stomach and intestinal inflammation, often viral",
        "specialist": "General Physician"
    },
    frozenset(["high fever", "severe headache", "neck stiffness", "sensitivity to light"]): {
        "disease": "Meningitis",
        "confidence": 0.80, "severity": "severe",
        "description": "EMERGENCY: Inflammation of brain/spinal cord membranes",
        "specialist": "Neurologist (EMERGENCY)"
    },
}

EMERGENCY_SYMPTOMS = {
    "chest pain", "difficulty breathing", "shortness of breath",
    "loss of consciousness", "severe bleeding", "stroke symptoms"
}


@router.post("/symptom-check", response_model=SymptomCheckResponse)
async def check_symptoms(request: SymptomCheckRequest):
    """
    Analyze symptoms and return possible diagnoses.

    Algorithm:
    1. Normalize symptom strings (lowercase, strip)
    2. Check if any emergency symptoms present
    3. Match against knowledge base using set intersection scoring
    4. If OpenAI key available → enrich with GPT analysis
    5. Return ranked possibilities with advice
    """
    # Normalize input symptoms
    input_symptoms = {s.lower().strip() for s in request.symptoms}

    # ── STEP 1: Emergency check ──
    emergency_match = input_symptoms & EMERGENCY_SYMPTOMS
    seek_immediate = bool(emergency_match)

    # ── STEP 2: Rule-based matching ──
    matches = []
    for symptom_set, disease_info in SYMPTOM_DB.items():
        # Jaccard similarity: |intersection| / |union|
        intersection = input_symptoms & symptom_set
        union = input_symptoms | symptom_set
        similarity = len(intersection) / len(union) if union else 0

        if similarity > 0.2 or len(intersection) >= 2:
            # Adjust confidence based on match quality
            adjusted_confidence = min(0.95, disease_info["confidence"] * (1 + similarity))
            matches.append(PossibleDisease(
                disease=disease_info["disease"],
                confidence=round(adjusted_confidence, 2),
                severity=disease_info["severity"],
                description=disease_info["description"],
                recommended_specialist=disease_info["specialist"]
            ))

    # Sort by confidence descending
    matches.sort(key=lambda x: x.confidence, reverse=True)

    # ── STEP 3: Try OpenAI enrichment ──
    openai_key = os.getenv("OPENAI_API_KEY")
    if openai_key and not matches:
        try:
            matches = await _enrich_with_openai(request, openai_key)
        except Exception:
            pass  # Fall back to rule-based results

    # Default if no matches found
    if not matches:
        matches = [PossibleDisease(
            disease="Undetermined — Consult a Doctor",
            confidence=0.5,
            severity="unknown",
            description="Symptoms didn't match known patterns. Please consult a physician.",
            recommended_specialist="General Physician"
        )]

    # ── STEP 4: Determine urgency ──
    max_confidence = matches[0].confidence if matches else 0
    has_severe = any(m.severity == "severe" for m in matches[:3])

    if seek_immediate or has_severe:
        urgency = "emergency"
    elif max_confidence > 0.7:
        urgency = "urgent"
    else:
        urgency = "routine"

    general_advice = _build_advice(input_symptoms, request.duration_days)

    return SymptomCheckResponse(
        possible_diseases=matches[:5],     # Top 5 results
        urgency_level=urgency,
        seek_immediate_care=seek_immediate,
        general_advice=general_advice,
        disclaimer=(
            "⚠️ This AI tool is for informational purposes ONLY. "
            "It is NOT a substitute for professional medical diagnosis. "
            "Always consult a qualified healthcare provider."
        )
    )


async def _enrich_with_openai(request: SymptomCheckRequest, api_key: str):
    """Call OpenAI GPT to analyze symptoms when rule-base has no matches."""
    try:
        import openai
        client = openai.AsyncOpenAI(api_key=api_key)

        prompt = f"""You are a medical assistant. A patient presents with the following symptoms:
Symptoms: {', '.join(request.symptoms)}
Age: {request.patient_age or 'unknown'}
Gender: {request.patient_gender or 'unknown'}
Duration: {request.duration_days or 'unknown'} days

List the 3 most likely conditions in JSON format:
[{{"disease": "...", "confidence": 0.0-1.0, "severity": "mild/moderate/severe", 
   "description": "...", "recommended_specialist": "..."}}]
Respond with JSON array only."""

        response = await client.chat.completions.create(
            model="gpt-3.5-turbo",
            messages=[
                {"role": "system", "content": "You are a knowledgeable medical assistant. Only output valid JSON."},
                {"role": "user", "content": prompt}
            ],
            max_tokens=600,
            temperature=0.3    # Low temperature = more consistent/factual
        )

        raw = response.choices[0].message.content.strip()
        parsed = json.loads(raw)
        return [PossibleDisease(**item) for item in parsed]
    except Exception:
        return []


def _build_advice(symptoms: set, duration_days) -> str:
    advice = []
    if "fever" in symptoms:
        advice.append("Stay hydrated and rest. Monitor temperature every 4 hours.")
    if "cough" in symptoms:
        advice.append("Avoid smoking and dusty environments. Consider steam inhalation.")
    if "headache" in symptoms:
        advice.append("Rest in a quiet, dark room. Stay hydrated.")
    if duration_days and duration_days > 7:
        advice.append("Symptoms lasting more than 7 days warrant a doctor's visit.")

    base = "Monitor your symptoms and seek medical care if they worsen."
    return base + (" " + " ".join(advice) if advice else "")
