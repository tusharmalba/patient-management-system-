"""
Healthcare Chatbot Router.
Conversational AI for healthcare Q&A using OpenAI GPT or rule-based fallback.
Maintains conversation history for context-aware responses.
"""

import os
from fastapi import APIRouter
from models.schemas import ChatRequest, ChatResponse

router = APIRouter()

HEALTHCARE_SYSTEM_PROMPT = """You are a helpful healthcare assistant for a Patient Management System.
You help patients and doctors with:
- General health information and wellness tips
- Explaining medical terms in plain language
- Understanding medications and their general uses
- Guidance on when to seek medical care
- Appointment and treatment process questions

IMPORTANT RULES:
1. NEVER diagnose specific conditions — say "consult your doctor"
2. NEVER recommend specific prescription medications
3. NEVER replace professional medical advice
4. Always recommend consulting a healthcare professional for serious concerns
5. Be empathetic, clear, and supportive
6. Keep responses concise (under 200 words)"""

# Rule-based fallback responses
RULE_BASED_RESPONSES = {
    "appointment": "You can book an appointment through the Appointments section. Choose your preferred doctor and select an available time slot. Make sure to describe your symptoms when booking.",
    "prescription": "Prescriptions are provided by doctors during or after appointments. You can view your prescriptions in your appointment history under the Appointments section.",
    "blood test": "Blood tests are ordered by your doctor. Results typically appear in your Medical Reports section within 24-48 hours. Your doctor will explain the results at your next appointment.",
    "emergency": "For medical emergencies, call emergency services (112/911) immediately. Do not rely on this system for emergency care.",
    "doctor": "You can view available doctors in the Doctors section, filter by specialization, and check their availability. Book an appointment with your preferred specialist.",
    "risk": "Your health risk score is calculated based on age, diabetes, smoking status, heart disease, and blood pressure. You can view it on your patient profile page.",
}


@router.post("/chat", response_model=ChatResponse)
async def chat(request: ChatRequest):
    """
    Multi-turn healthcare chatbot.
    Sends full conversation history to maintain context across turns.
    """
    openai_key = os.getenv("OPENAI_API_KEY")

    if openai_key:
        try:
            return await _chat_with_openai(request, openai_key)
        except Exception:
            pass

    return _rule_based_chat(request)


async def _chat_with_openai(request: ChatRequest, api_key: str) -> ChatResponse:
    import openai
    client = openai.AsyncOpenAI(api_key=api_key)

    # Build messages list: system prompt + history + current message
    messages = [{"role": "system", "content": HEALTHCARE_SYSTEM_PROMPT}]

    # Add conversation history (for multi-turn context)
    for msg in request.conversation_history[-10:]:   # Last 10 messages max
        messages.append({"role": msg.role, "content": msg.content})

    # Add patient context if provided
    if request.patient_context:
        context_str = f"\nPatient context: {request.patient_context}"
        messages[-1]["content"] = messages[-1]["content"] + context_str if messages else context_str

    messages.append({"role": "user", "content": request.message})

    response = await client.chat.completions.create(
        model="gpt-3.5-turbo",
        messages=messages,
        max_tokens=400,
        temperature=0.7
    )

    reply = response.choices[0].message.content

    follow_ups = _generate_follow_ups(request.message)

    return ChatResponse(
        reply=reply,
        sources_consulted=["OpenAI GPT-3.5-turbo", "Healthcare guidelines"],
        follow_up_questions=follow_ups,
        disclaimer="This AI assistant provides general health information only. Always consult a qualified healthcare provider for medical advice."
    )


def _rule_based_chat(request: ChatRequest) -> ChatResponse:
    msg_lower = request.message.lower()

    # Find matching rule
    for keyword, response in RULE_BASED_RESPONSES.items():
        if keyword in msg_lower:
            return ChatResponse(
                reply=response,
                follow_up_questions=_generate_follow_ups(request.message),
                disclaimer="This is general information only. Consult your doctor for medical advice."
            )

    # Default response
    return ChatResponse(
        reply=(
            "I'm here to help with general healthcare questions about this system. "
            "I can assist with appointments, understanding your health records, "
            "general wellness tips, and navigating the patient portal. "
            "For specific medical advice, please consult your doctor directly."
        ),
        follow_up_questions=[
            "How do I book an appointment?",
            "Where can I view my medical reports?",
            "What does my risk score mean?"
        ],
        disclaimer="This AI assistant provides general information only. Always consult a qualified healthcare provider."
    )


def _generate_follow_ups(message: str) -> list:
    msg_lower = message.lower()
    if any(w in msg_lower for w in ["doctor", "appointment", "book"]):
        return ["How do I choose the right specialist?", "What should I bring to my appointment?"]
    if any(w in msg_lower for w in ["medicine", "drug", "medication"]):
        return ["How do I view my prescription history?", "What are common drug interactions?"]
    if any(w in msg_lower for w in ["test", "blood", "report"]):
        return ["How long do test results take?", "What do my lab values mean?"]
    return ["Can you explain that in simpler terms?", "What should I do next?"]
