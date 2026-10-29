from fastapi import APIRouter, Depends
from app.schemas import AnalyzeRequest, AnalyzeResponse
from app.api.dependencies import get_conversation_analyzer
from app.analyzers.conversation import ConversationAnalyzer

router = APIRouter()


@router.get("/health")
def health():
    return {"status": "ok"}


@router.post("/v1/analyze")
def analyze(analyze_request: AnalyzeRequest, analyzer: ConversationAnalyzer = Depends(get_conversation_analyzer),
) -> AnalyzeResponse:
    return analyzer.analyze(analyze_request)


@router.post("/v1/evaluate")
def evaluate():
    # TODO: accept an EvaluateRequest (app/schemas.py) as the body
    # TODO: look up the form by formId; unknown formId -> 404
    # TODO: get the evaluator with Depends(...) and run it
    # TODO: return an EvaluateResponse
    ...
