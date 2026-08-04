from typing import Literal, Optional

from pydantic import BaseModel, Field

Category = Literal["BUG", "FEATURE_REQUEST", "SUPPORT", "BILLING", "OTHER"]
Urgency = Literal["LOW", "MEDIUM", "HIGH", "URGENT"]


class ClassifyRequest(BaseModel):
    title: str
    description: Optional[str] = None


class ClassifyResponse(BaseModel):
    category: Category
    urgency: Urgency
    confidence: float = Field(ge=0.0, le=1.0)
    reasoning: str
