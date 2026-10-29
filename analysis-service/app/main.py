"""Entry point. uvicorn runs `app.main:app`: the variable `app` in this file."""

import logging
import os
from collections.abc import AsyncGenerator
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.analyzers.conversation import ConversationAnalyzer
from app.analyzers.fakes import FakeEmpathyClassifier, FakeSentimentClassifier
from app.api.routes import router

logger = logging.getLogger("uvicorn.error")  # uvicorn's logger, so messages show in its output


def build_analyzer() -> ConversationAnalyzer:
    """The analyzer every request shares. CALLSCOPE_MODELS picks the classifiers: "fake" (default) or "real"."""
    models = os.environ.get("CALLSCOPE_MODELS", "fake")
    if models == "real":
        # Imported only here, so the fakes work without the ML libraries installed (tests, CI).
        from app.analyzers.empathy import HuggingFaceEmpathyClassifier
        from app.analyzers.sentiment import HuggingFaceSentimentClassifier

        analyzer = ConversationAnalyzer(HuggingFaceSentimentClassifier(), HuggingFaceEmpathyClassifier())
    elif models == "fake":
        analyzer = ConversationAnalyzer(FakeSentimentClassifier(), FakeEmpathyClassifier())
    else:
        # Fail at startup, not on the first request: a typo should never silently mean "fake".
        raise ValueError(f'CALLSCOPE_MODELS must be "fake" or "real", not {models!r}')

    logger.info("Classifiers loaded: %s, %s", analyzer.sentiment.model_version, analyzer.empathy.model_version)
    return analyzer


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
