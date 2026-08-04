from fastapi import FastAPI

from app.classifier import classify
from app.schemas import ClassifyRequest, ClassifyResponse

app = FastAPI(title="TaskTriage Classification Service")


@app.get("/health")
def health() -> dict:
    return {"status": "UP"}


@app.post("/classify", response_model=ClassifyResponse)
def classify_task(request: ClassifyRequest) -> ClassifyResponse:
    return classify(request)
