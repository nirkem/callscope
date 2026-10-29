"""The real sentiment model. Skipped where the ML libraries aren't installed (CI installs only .[dev])."""

import pytest

pytest.importorskip("transformers")

from app.analyzers.sentiment import HuggingFaceSentimentClassifier  # noqa: E402
from app.schemas import SentimentLabel  # noqa: E402


@pytest.fixture(scope="module")
def classifier() -> HuggingFaceSentimentClassifier:
    # Loading takes a second or two, so load once for all tests in this file.
    return HuggingFaceSentimentClassifier()


def test_clear_cases(classifier):
    preds = classifier.classify(["Thanks, that's great", "My order never arrived", "Where is my package?"])
    assert [p.label for p in preds] == [SentimentLabel.POSITIVE, SentimentLabel.NEGATIVE, SentimentLabel.NEUTRAL]


def test_keeps_the_base_promises(classifier):
    texts = ["one", "two", "three"]
    preds = classifier.classify(texts)
    assert len(preds) == len(texts)
    assert classifier.classify([]) == []
    assert all(0 <= p.confidence <= 1 for p in preds)


def test_turn_longer_than_the_model_limit_does_not_crash(classifier):
    # Regression test: without max_length, the tokenizer doesn't truncate and the model fails at 514 tokens.
    assert len(classifier.classify(["word " * 3000])) == 1
