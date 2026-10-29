from abc import ABC, abstractmethod
from dataclasses import dataclass


@dataclass(frozen=True)
class Prediction[L]:
    """What a model said about one text, and how sure it was."""

    label: L
    confidence: float


class TextClassifier[L](ABC):
    """Gives each text a label of type L. Subclasses wrap a real model or a fake."""

    @abstractmethod
    def classify(self, texts: list[str]) -> list[Prediction[L]]:
        """Classify every text in one call.

        Promises:
          - the output has the same length and order as the input,
          - an empty list in gives an empty list out,
          - every confidence is between 0 and 1.
        """

    @property
    @abstractmethod
    def model_version(self) -> str:
        """Name of the model behind this classifier."""
