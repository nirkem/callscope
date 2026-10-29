"""Fake classifiers: predictable labels from keywords, no model download. For tests and local runs."""

from app.analyzers.base import Prediction, TextClassifier
from app.schemas import EmpathyLabel, SentimentLabel


class FakeSentimentClassifier(TextClassifier[SentimentLabel]):
    """Sentiment from keywords."""

    NEGATIVE_WORDS = {"never", "angry"}
    POSITIVE_WORDS = {"thanks", "great"}

    def classify(self, texts: list[str]) -> list[Prediction[SentimentLabel]]:
        preds = []
        for text in texts:
            text = text.lower()

            # Negative first: a complaint with polite words is still a complaint.
            if any(word in text for word in self.NEGATIVE_WORDS):
                label = SentimentLabel.NEGATIVE
                confidence = 0.9
            elif any(word in text for word in self.POSITIVE_WORDS):
                label = SentimentLabel.POSITIVE
                confidence = 0.9
            else:
                label = SentimentLabel.NEUTRAL
                confidence = 0.6

            preds.append(Prediction(label, confidence))

        return preds

    @property
    def model_version(self) -> str:
        return "fake-sentiment-v1"


class FakeEmpathyClassifier(TextClassifier[EmpathyLabel]):
    """Empathy from keywords."""

    EMPATHETIC_WORDS = {"sorry", "understand"}
    # Both apostrophes: text copied from phones or Word often has the curly one.
    UNHELPFUL_WORDS = {"policy", "can't", "can’t"}

    def classify(self, texts: list[str]) -> list[Prediction[EmpathyLabel]]:
        preds = []
        for text in texts:
            text = text.lower()

            # Unhelpful first: "Sorry, but that's our policy" is still unhelpful.
            if any(word in text for word in self.UNHELPFUL_WORDS):
                label = EmpathyLabel.UNHELPFUL
                confidence = 0.9
            elif any(word in text for word in self.EMPATHETIC_WORDS):
                label = EmpathyLabel.EMPATHETIC
                confidence = 0.9
            else:
                label = EmpathyLabel.NEUTRAL
                confidence = 0.6

            preds.append(Prediction(label, confidence))

        return preds

    @property
    def model_version(self) -> str:
        return "fake-empathy-v1"
