"""Entry point. uvicorn runs `app.main:app`: the variable `app` in this file."""

from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.analyzers.conversation import ConversationAnalyzer
from app.analyzers.fakes import FakeEmpathyClassifier, FakeSentimentClassifier
from app.api.routes import router


def build_analyzer() -> ConversationAnalyzer:
    """The analyzer every request shares. Day 2: real models instead of the fakes."""
    return ConversationAnalyzer(FakeSentimentClassifier(), FakeEmpathyClassifier())


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    # Before yield: runs once at startup. Real models take seconds to load, so never per request.
    # dependencies.get_conversation_analyzer reads it from app.state.
    app.state.conversation_analyzer = build_analyzer()

    yield

    # After yield: runs once at shutdown. Nothing to release yet.


app = FastAPI(
    title="CallScope analysis-service",
    version="0.1.0",
    description="Sentiment and empathy analysis for contact-center conversations.",
    lifespan=lifespan,
)
app.include_router(router)
