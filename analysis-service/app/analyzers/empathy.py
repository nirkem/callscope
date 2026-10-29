from transformers import pipeline

from app.analyzers.base import Prediction, TextClassifier
from app.schemas import EmpathyLabel

MODEL_NAME = "facebook/bart-large-mnli"
BATCH_SIZE = 8  # smaller than sentiment: bart is ~3x bigger, and zero-shot runs it once per label
HYPOTHESIS_TEMPLATE = "The agent's reply is {}."  # each phrase below is put into the {}

# Our choice, not the model's: the phrases each reply is compared against, mapped to our labels.
# Wording matters; these and HYPOTHESIS_TEMPLATE are the knobs to tune for accuracy.
EMPATHY_PHRASES = {
    "empathetic and caring": EmpathyLabel.EMPATHETIC,
    "neutral": EmpathyLabel.NEUTRAL,
    "dismissive or unhelpful": EmpathyLabel.UNHELPFUL,
}


class HuggingFaceEmpathyClassifier(TextClassifier[EmpathyLabel]):
    """Empathy of agent replies: zero-shot classification with bart-large-mnli, run locally."""

    def __init__(self) -> None:
        # Slow: loads the model (downloads it the first time). Build once, at startup.
        self.pipeline = pipeline("zero-shot-classification", model=MODEL_NAME)

    def classify(self, texts: list[str]) -> list[Prediction[EmpathyLabel]]:
        """Labels every text with one batched model call."""
        if not texts:
            return []

        # No truncation settings needed: the zero-shot pipeline truncates long turns by itself.
        results = self.pipeline(
            texts,
            batch_size=BATCH_SIZE,
            candidate_labels=list(EMPATHY_PHRASES),
            hypothesis_template=HYPOTHESIS_TEMPLATE,
        )
        # Each result lists every phrase, best first: take the winner and its score.
        return [Prediction(EMPATHY_PHRASES[r["labels"][0]], r["scores"][0]) for r in results]

    @property
    def model_version(self) -> str:
        return MODEL_NAME
