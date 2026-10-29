from fastapi import Request
from app.analyzers.conversation import ConversationAnalyzer

def get_conversation_analyzer(request: Request) -> ConversationAnalyzer:
    return request.app.state.conversation_analyzer