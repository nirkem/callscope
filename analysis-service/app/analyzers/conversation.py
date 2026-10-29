from app.analyzers import scoring
from app.analyzers.base import Prediction, TextClassifier
from app.schemas import (
    AnalyzeRequest,
    AnalyzeResponse,
    EmpathyLabel,
    ModelVersions,
    SentimentLabel,
    Speaker,
    Summary,
    Turn,
    TurnResult,
)

# A turn's position in the conversation, and its text.
IndexedText = tuple[int, str]


class ConversationAnalyzer:
    """Turns a conversation into per-turn labels and a summary.
    Receives its classifiers instead of creating them: fakes in tests, real models in production.
    """

    def __init__(
        self,
        sentiment: TextClassifier[SentimentLabel],
        empathy: TextClassifier[EmpathyLabel],
    ) -> None:
        self.sentiment = sentiment
        self.empathy = empathy

    def analyze(self, request: AnalyzeRequest) -> AnalyzeResponse:
        customer = _turns_by(request.turns, Speaker.CUSTOMER)
        agent = _turns_by(request.turns, Speaker.AGENT)

        # One batched call per classifier, however long the conversation is.
        sentiment_preds = self.sentiment.classify([text for _, text in customer])
        empathy_preds = self.empathy.classify([text for _, text in agent])

        return AnalyzeResponse(
            conversation_id=request.conversation_id,
            model_versions=ModelVersions(
                sentiment=self.sentiment.model_version,
                empathy=self.empathy.model_version,
            ),
            turns=_turn_results(customer, sentiment_preds, agent, empathy_preds),
            summary=_summary(sentiment_preds, empathy_preds),
        )


def _turns_by(turns: list[Turn], speaker: Speaker) -> list[IndexedText]:
    """The turns one speaker said, each with its index in the conversation."""
    return [(index, turn.text) for index, turn in enumerate(turns) if turn.speaker == speaker]


def _turn_results(
    customer: list[IndexedText],
    sentiment_preds: list[Prediction[SentimentLabel]],
    agent: list[IndexedText],
    empathy_preds: list[Prediction[EmpathyLabel]],
) -> list[TurnResult]:
    """One TurnResult per turn, back in the original order."""
    results = [
        TurnResult(index=index, speaker=Speaker.CUSTOMER, sentiment=p.label, confidence=p.confidence)
        for (index, _), p in zip(customer, sentiment_preds)
    ] + [
        TurnResult(index=index, speaker=Speaker.AGENT, empathy=p.label, confidence=p.confidence)
        for (index, _), p in zip(agent, empathy_preds)
    ]
    return sorted(results, key=lambda r: r.index)


def _summary(
    sentiment_preds: list[Prediction[SentimentLabel]],
    empathy_preds: list[Prediction[EmpathyLabel]],
) -> Summary:
    """The three conversation-level numbers."""
    customer_scores = [scoring.score(p, scoring.SENTIMENT_VALUES) for p in sentiment_preds]
    agent_scores = [scoring.score(p, scoring.EMPATHY_VALUES) for p in empathy_preds]
    return Summary(
        customer_sentiment=scoring.customer_sentiment(customer_scores),
        sentiment_trend=scoring.sentiment_trend(customer_scores),
        agent_empathy=scoring.agent_empathy(agent_scores),
    )
