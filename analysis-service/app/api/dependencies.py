from fastapi import Request

from app.analyzers.conversation import ConversationAnalyzer


def get_conversation_analyzer(request: Request) -> ConversationAnalyzer:
    """The analyzer main.py built at startup. Tests replace it with app.dependency_overrides."""
    return request.app.state.conversation_analyzer
