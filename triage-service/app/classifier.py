import anthropic

from app.config import settings
from app.schemas import ClassifyRequest, ClassifyResponse

_client = anthropic.Anthropic(api_key=settings.anthropic_api_key)

_SYSTEM_PROMPT = (
    "You triage incoming work requests for a freelancer/small team task tool. "
    "Classify the request's category and urgency, and give a confidence score "
    "for your own classification (0.0 = pure guess, 1.0 = very certain). "
    "Categories: BUG (something broken), FEATURE_REQUEST (new capability), "
    "SUPPORT (help or a question), BILLING (payment or invoice related), OTHER. "
    "Urgency: LOW, MEDIUM, HIGH, URGENT."
)

# Claude의 구조화 출력(output_config.format) 기능으로 응답이 항상 이 스키마를
# 따르도록 강제한다 — 파싱 실패로 인한 재시도/예외 처리를 신경 쓸 필요가 없다.
_RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "category": {
            "type": "string",
            "enum": ["BUG", "FEATURE_REQUEST", "SUPPORT", "BILLING", "OTHER"],
        },
        "urgency": {
            "type": "string",
            "enum": ["LOW", "MEDIUM", "HIGH", "URGENT"],
        },
        "confidence": {"type": "number"},
        "reasoning": {"type": "string"},
    },
    "required": ["category", "urgency", "confidence", "reasoning"],
    "additionalProperties": False,
}


def classify(request: ClassifyRequest) -> ClassifyResponse:
    user_content = f"Title: {request.title}\nDescription: {request.description or '(none)'}"

    response = _client.messages.create(
        model=settings.classify_model,
        max_tokens=512,
        system=_SYSTEM_PROMPT,
        messages=[{"role": "user", "content": user_content}],
        output_config={
            "format": {
                "type": "json_schema",
                "schema": _RESPONSE_SCHEMA,
            }
        },
    )

    text = next(block.text for block in response.content if block.type == "text")
    return ClassifyResponse.model_validate_json(text)
