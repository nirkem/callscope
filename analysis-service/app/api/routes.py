from fastapi import APIRouter, Depends

from app.analyzers.conversation import ConversationAnalyzer
from app.api.dependencies import get_conversation_analyzer
from app.schemas import AnalyzeRequest, AnalyzeResponse

router = APIRouter()


@router.get("/health")
def health():
    return {"status": "ok"}


@router.post("/v1/analyze")
def analyze(
    analyze_request: AnalyzeRequest,
    analyzer: ConversationAnalyzer = Depends(get_conversation_analyzer),
) -> AnalyzeResponse:
    return analyzer.analyze(analyze_request)
