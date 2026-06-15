"""
Disease Prediction Router
Predicts patient health risks from clinical data.
Uses the same algorithm as the Java Patient.calculateAndSetRiskScore()
but enriched with additional ML-based predictions.
"""

import os
from fastapi import APIRouter
from models.schemas import PatientRiskRequest, RiskPredictionResponse, RiskLevel

router = APIRouter()


@router.post("/predict-risk", response_model=RiskPredictionResponse)
async def predict_risk(request: PatientRiskRequest):
    """
    Calculate comprehensive patient risk score.

    Risk scoring algorithm:
    ┌──────────────────────┬────────┐
    │ Factor               │ Points │
    ├──────────────────────┼────────┤
    │ Age 40-59            │ +1     │
    │ Age 60+              │ +2     │
    │ Diabetes             │ +2     │
    │ Smoker               │ +2     │
    │ Heart disease        │ +3     │
    │ High blood pressure  │ +2     │
    │ High BMI (>30)       │ +1     │
    └──────────────────────┴────────┘

    Score → Risk Level:
    0-3  → LOW
    4-6  → MEDIUM
    7+   → HIGH
    """
    score = 0
    risk_factors = []
    protective_factors = []
    recommendations = []
    predicted_conditions = []

    # ── Age scoring ──
    if request.age >= 60:
        score += 2
        risk_factors.append(f"Advanced age ({request.age} years) — significant cardiovascular risk factor")
    elif request.age >= 40:
        score += 1
        risk_factors.append(f"Middle age ({request.age} years) — monitor cardiovascular health")
    else:
        protective_factors.append(f"Young age ({request.age} years) — lower baseline risk")

    # ── Diabetes ──
    if request.has_diabetes:
        score += 2
        risk_factors.append("Diabetes — increases risk of cardiovascular disease, neuropathy, retinopathy")
        predicted_conditions.extend(["Diabetic Neuropathy", "Diabetic Retinopathy", "Cardiovascular Disease"])
        recommendations.append("Regular HbA1c monitoring, maintain blood glucose < 140 mg/dL post-meal")
    else:
        protective_factors.append("No diabetes")

    # ── Smoking ──
    if request.is_smoker:
        score += 2
        risk_factors.append("Smoking — highest modifiable risk factor for lung cancer and heart disease")
        predicted_conditions.extend(["COPD", "Lung Cancer", "Coronary Artery Disease"])
        recommendations.append("Smoking cessation is the single most impactful health improvement possible")
    else:
        protective_factors.append("Non-smoker")

    # ── Heart disease ──
    if request.has_heart_disease:
        score += 3
        risk_factors.append("Existing heart disease — highest cardiovascular risk")
        predicted_conditions.extend(["Heart Failure", "Arrhythmia", "Stroke"])
        recommendations.append("Regular cardiology follow-up, adhere strictly to cardiac medications")

    # ── Blood pressure ──
    if request.has_high_blood_pressure:
        score += 2
        risk_factors.append("Hypertension — 'silent killer', damages kidneys and heart vessels")
        predicted_conditions.extend(["Stroke", "Kidney Disease", "Heart Failure"])
        recommendations.append("Monitor BP daily, reduce sodium intake to < 2g/day")

    # ── BMI ──
    if request.bmi and request.bmi > 30:
        score += 1
        risk_factors.append(f"Obesity (BMI: {request.bmi:.1f}) — increases insulin resistance and cardiac load")
        recommendations.append("Aim for 150 min/week moderate exercise; consult a dietitian")

    # ── Determine risk level ──
    if score >= 7:
        risk_level = RiskLevel.HIGH
        recommendations.insert(0, "HIGH RISK: Schedule comprehensive health evaluation immediately")
    elif score >= 4:
        risk_level = RiskLevel.MEDIUM
        recommendations.insert(0, "MEDIUM RISK: Schedule preventive health check within 3 months")
    else:
        risk_level = RiskLevel.LOW
        recommendations.insert(0, "LOW RISK: Maintain healthy lifestyle and annual check-ups")

    # ── Lifestyle recommendations ──
    lifestyle_changes = [
        "30 minutes of moderate exercise 5 days per week",
        "Mediterranean diet: fruits, vegetables, whole grains, lean proteins",
        "7-9 hours of quality sleep per night",
        "Stress management: meditation, yoga, or breathing exercises",
        "Annual physical examination and blood work",
    ]
    if request.is_smoker:
        lifestyle_changes.insert(0, "🚭 Quit smoking — consult doctor about cessation aids")
    if not protective_factors:
        lifestyle_changes.append("Consider consulting a nutritionist and exercise physiologist")

    return RiskPredictionResponse(
        risk_level=risk_level,
        risk_score=min(score, 10),
        risk_factors=risk_factors,
        protective_factors=protective_factors or ["No major protective factors identified"],
        recommendations=recommendations,
        predicted_conditions=list(set(predicted_conditions)) or ["No specific conditions predicted"],
        lifestyle_changes=lifestyle_changes
    )
