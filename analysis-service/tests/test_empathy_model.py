"""The real empathy model. Skipped where the ML libraries aren't installed (CI installs only .[dev])."""

import pytest

pytest.importorskip("transformers")

from app.analyzers.empathy import HuggingFaceEmpathyClassifier  # noqa: E402
from app.schemas import EmpathyLabel  # noqa: E402


@pytest.fixture(scope="module")
def classifier() -> HuggingFaceEmpathyClassifier:
    # bart-large-mnli takes several seconds to load, so load once for all tests in this file.
    return HuggingFaceEmpathyClassifier()


def test_clear_cases(classifier):
    preds = classifier.classify([
        "I'm so sorry about that, let me fix it right away.",
        "That's our policy, I can't change it.",
        "Can you confirm the email on the account?",
    ])
    assert [p.label for p in preds] == [EmpathyLabel.EMPATHETIC, EmpathyLabel.UNHELPFUL, EmpathyLabel.NEUTRAL]


def test_an_apology_is_empathetic_not_negative(classifier):
    # Why empathy needs its own model: sentiment calls this NEGATIVE because of "frustrating".
    [pred] = classifier.classify(["I understand how frustrating that must be."])
    assert pred.label == EmpathyLabel.EMPATHETIC


def test_keeps_the_base_promises(classifier):
    texts = ["Hello.", "Let me check.", "Goodbye."]
    preds = classifier.classify(texts)
    assert len(preds) == len(texts)
    assert classifier.classify([]) == []
    assert all(0 <= p.confidence <= 1 for p in preds)


def test_turn_longer_than_the_model_limit_does_not_crash(classifier):
    assert len(classifier.classify(["word " * 3000])) == 1
