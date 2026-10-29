"""Conversation-level scores. Plain functions with no models, so they're easy to test with hand-made numbers."""

import math

from app.analyzers.base import Prediction
from app.schemas import EmpathyLabel, SentimentLabel

# Each label as a number. A prediction's score is label value * confidence, so it's in -1..+1.
SENTIMENT_VALUES = {
    SentimentLabel.POSITIVE: 1,
    SentimentLabel.NEUTRAL: 0,
    SentimentLabel.NEGATIVE: -1,
}
EMPATHY_VALUES = {
    EmpathyLabel.EMPATHETIC: 1,
    EmpathyLabel.NEUTRAL: 0,
    EmpathyLabel.UNHELPFUL: -1,
}

END_SHARE = 0.15  # the last 15% of customer turns...
END_WEIGHT = 2  # ...count double


def score(prediction: Prediction, values: dict) -> float:
    """One prediction as a number from -1 to +1."""
    return values[prediction.label] * prediction.confidence


def customer_sentiment(scores: list[float]) -> int | None:
    """Weighted average of customer scores, as -100..+100. The end of the call counts double."""
    if not scores:
        return None

    n = len(scores)
    end_turns = math.ceil(n * END_SHARE)  # at least 1, since ceil rounds up
    weights = [END_WEIGHT if i >= n - end_turns else 1 for i in range(n)]

    weighted_sum = sum(w * s for w, s in zip(weights, scores))
    return round(weighted_sum / sum(weights) * 100)


def sentiment_trend(scores: list[float]) -> int | None:
    """Second half vs. first half of the customer turns, as -100..+100. Positive = the call improved."""
    if len(scores) < 2:
        return None

    middle = len(scores) // 2  # with an odd count, the middle turn goes to the second half
    first_half, second_half = scores[:middle], scores[middle:]

    avg_first = sum(first_half) / len(first_half)
    avg_second = sum(second_half) / len(second_half)
    return round((avg_second - avg_first) * 100 / 2)


def agent_empathy(scores: list[float]) -> int | None:
    """Plain average of agent scores, as -100..+100."""
    if not scores:
        return None

    return round(sum(scores) / len(scores) * 100)
