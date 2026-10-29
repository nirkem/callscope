"""
Request and response shapes for analysis-service.

Every class here describes JSON that crosses the HTTP boundary. FastAPI uses
them to:
  - validate incoming requests (bad input gets an automatic 422 response,
    so the analyzers never see invalid data),
  - turn outgoing Python objects into JSON,
  - document the API on the /docs page.

These classes are the source of truth for the HTTP contracts. FastAPI publishes
them as OpenAPI at /openapi.json. The example messages in callscope/contracts/examples/
must load into them; contract tests check that on both the Python and Java side.
"""

# StrEnum (Python 3.11+): an enum whose members are also plain strings,
# so Speaker.AGENT == "AGENT" and it becomes "AGENT" in JSON with no extra work.
from enum import StrEnum

# BaseModel: the Pydantic base class. A subclass declares fields with type hints,
#            and Pydantic checks every value against those types.
# ConfigDict: settings that change how a model behaves (see CamelModel below).
# Field:      extra rules for a single field, such as length limits.
from pydantic import BaseModel, ConfigDict, Field

# to_camel: turns "conversation_id" into "conversationId".
from pydantic.alias_generators import to_camel


# ---------------------------------------------------------------------------
# Limits
# ---------------------------------------------------------------------------
# These protect the service. Without them, a single huge request could keep
# the model busy for a minute and slow down every other request.
# They live in named constants so they're easy to find, change and test.

MAX_TURNS = 500  # most turns allowed in one conversation
MAX_TURN_CHARS = 5_000  # most characters allowed in one turn (5_000 == 5000)


# ---------------------------------------------------------------------------
# Shared base class
# ---------------------------------------------------------------------------


class CamelModel(BaseModel):
    """Base for every schema: camelCase in JSON, snake_case in Python."""

    # model_config applies to this class AND every class that inherits from it.
    # Setting it once here means no schema can forget it.
    model_config = ConfigDict(
        # Python style is snake_case (conversation_id); the JSON contracts use
        # camelCase (conversationId). Each field gets a camelCase "alias", which
        # is the name used when reading or writing JSON.
        alias_generator=to_camel,
        # Also accept the Python name when building an object in code, e.g.
        # AnalyzeRequest(conversation_id="c-1", ...). Useful in tests.
        populate_by_name=True,
        # Reject unknown fields instead of silently ignoring them. A typo like
        # "conversationID" fails loudly.
        extra="forbid",
        # Trim spaces at the start and end of every string BEFORE the length
        # rules run, so "   " becomes "" and fails min_length=1.
        str_strip_whitespace=True,
    )


# ---------------------------------------------------------------------------
# Shared pieces
# ---------------------------------------------------------------------------


class Speaker(StrEnum):
    """Who said a turn. Only these two values are accepted."""

    # Sentiment is measured on CUSTOMER turns, empathy on AGENT turns,
    # so every analyzer depends on this value being right.
    CUSTOMER = "CUSTOMER"
    AGENT = "AGENT"


class Turn(CamelModel):
    """One line of the conversation."""

    speaker: Speaker
    text: str = Field(min_length=1, max_length=MAX_TURN_CHARS)


# ---------------------------------------------------------------------------
# /v1/analyze
# ---------------------------------------------------------------------------


class AnalyzeRequest(CamelModel):
    """Body of POST /v1/analyze: a conversation to score for sentiment and empathy.

    Deliberately contains only what analysis needs. The SQS eventId is NOT here:
    queues are conversation-service's concern, and this service should not know
    they exist.
    """

    conversation_id: str = Field(min_length=1)
    turns: list[Turn] = Field(min_length=1, max_length=MAX_TURNS)


class SentimentLabel(StrEnum):
    """Sentiment of one customer turn. Only these three values are accepted."""

    POSITIVE = "POSITIVE"
    NEUTRAL = "NEUTRAL"
    NEGATIVE = "NEGATIVE"


class EmpathyLabel(StrEnum):
    """Empathy of one agent turn. Only these three values are accepted."""

    EMPATHETIC = "EMPATHETIC"
    NEUTRAL = "NEUTRAL"
    UNHELPFUL = "UNHELPFUL"


class TurnResult(CamelModel):
    """Result for one turn: sentiment for a customer turn, empathy for an agent turn."""

    index: int = Field(ge=0)
    speaker: Speaker
    sentiment: SentimentLabel | None = None
    empathy: EmpathyLabel | None = None
    confidence: float = Field(ge=0, le=1)


class Summary(CamelModel):
    """The whole conversation in three numbers. None when there is nothing to measure."""

    customer_sentiment: int | None = Field(default=None, ge=-100, le=100)
    sentiment_trend: int | None = Field(default=None, ge=-100, le=100)
    agent_empathy: int | None = Field(default=None, ge=-100, le=100)


class ModelVersions(CamelModel):
    """Which model produced each kind of result, so results stay comparable over time."""

    sentiment: str = Field(min_length=1)
    empathy: str = Field(min_length=1)


class AnalyzeResponse(CamelModel):
    """Body returned by POST /v1/analyze."""

    conversation_id: str = Field(min_length=1)
    model_versions: ModelVersions
    turns: list[TurnResult] = Field(min_length=1)
    summary: Summary
