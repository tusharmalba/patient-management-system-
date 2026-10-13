"""
Medical Report Summarizer Router.
Converts complex medical jargon into plain-language summaries.
Uses OpenAI if available, otherwise regex + rule-based parsing.
"""

import os
import re
from fastapi import APIRouter
from models.schemas import ReportSummaryRequest, ReportSummaryResponse

router = APIRouter()

# Common abnormal lab value patterns
ABNORMAL_PATTERNS = [
    (r'hemoglobin[:\s]+(\d+\.?\d*)\s*(g/dl)?', 'Hemoglobin', 12.0, 17.5),
    (r'blood\s+glucose[:\s]+(\d+\.?\d*)', 'Blood Glucose', 70, 100),
    (r'creatinine[:\s]+(\d+\.?\d*)', 'Creatinine', 0.6, 1.2),
    (r'cholesterol[:\s]+(\d+\.?\d*)', 'Total Cholesterol', 0, 200),
]


@router.post("/summarize-report", response_model=ReportSummaryResponse)
async def summarize_report(request: ReportSummaryRequest):
    """
    Summarize a medical report into patient-friendly language.
    Tries OpenAI GPT first; falls back to rule-based parsing.
    """
    openai_key = os.getenv("OPENAI_API_KEY")

    if openai_key:
        try:
            return await _summarize_with_openai(request, openai_key)
        except Exception:
            pass

    return _summarize_rule_based(request)


async def _summarize_with_openai(request: ReportSummaryRequest, api_key: str):
    import openai
    import json

    client = openai.AsyncOpenAI(api_key=api_key)
    prompt = f"""You are a medical report interpreter. Summarize this report in plain English.

Report Type: {request.report_type or 'medical report'}
Patient Age: {request.patient_age or 'unknown'}
Report Text:
{request.report_text[:3000]}

Respond with JSON:
{{
  "summary": "2-3 sentence plain English summary",
  "key_findings": ["finding1", "finding2"],
  "abnormal_values": ["any value outside normal range"],
  "recommendations": ["next step 1", "next step 2"],
  "urgency": "routine|follow_up|urgent",
  "simplified_explanation": "explain to a non-medical person in 1 paragraph"
}}"""

    response = await client.chat.completions.create(
        model="gpt-3.5-turbo",
        messages=[
            {"role": "system", "content": "You are a medical interpreter. Output valid JSON only."},
            {"role": "user", "content": prompt}
        ],
        max_tokens=800, temperature=0.2
    )

    data = json.loads(response.choices[0].message.content.strip())
    return ReportSummaryResponse(**data)


def _summarize_rule_based(request: ReportSummaryRequest) -> ReportSummaryResponse:
    """Fallback: keyword-based extraction when OpenAI is unavailable."""
    text = request.report_text.lower()

    # Detect abnormal values via regex
    abnormal = []
    for pattern, name, low, high in ABNORMAL_PATTERNS:
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            try:
                val = float(match.group(1))
                if val < low or val > high:
                    abnormal.append(f"{name}: {val} (normal: {low}-{high})")
            except ValueError:
                pass

    # Keyword-based findings
    findings = []
    if any(w in text for w in ["elevated", "high", "increased", "above normal"]):
        findings.append("Some values are elevated above normal range")
    if any(w in text for w in ["low", "decreased", "below normal", "deficiency"]):
        findings.append("Some values are below normal range")
    if any(w in text for w in ["normal", "within range", "unremarkable"]):
        findings.append("Several values are within normal limits")
    if any(w in text for w in ["infection", "bacteria", "viral"]):
        findings.append("Signs of infection detected")
    if any(w in text for w in ["inflammation", "inflammatory"]):
        findings.append("Inflammatory markers present")

    urgency = "urgent" if abnormal else "routine"

    return ReportSummaryResponse(
        summary=f"Medical report analysis for {request.report_type or 'patient'}. "
                f"{'Abnormal values detected requiring attention.' if abnormal else 'No critical abnormalities detected.'}",
        key_findings=findings or ["Report processed — consult your doctor for interpretation"],
        abnormal_values=abnormal,
        recommendations=[
            "Discuss these results with your doctor",
            "Follow up within 1 week if values are abnormal",
            "Maintain records for comparison in future tests"
        ],
        urgency=urgency,
        simplified_explanation=(
            "Your medical report has been analyzed. "
            + (f"There are {len(abnormal)} value(s) outside the normal range that your doctor should review. "
               if abnormal else "The values appear generally within acceptable ranges. ")
            + "Please consult your healthcare provider to discuss what these results mean for your health."
        )
    )
