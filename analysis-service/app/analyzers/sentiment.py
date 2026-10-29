from transformers import pipeline

from app.analyzers.base import Prediction, TextClassifier
from app.schemas import SentimentLabel

MODEL_NAME = "cardiffnlp/twitter-roberta-base-sentiment-latest"
BATCH_SIZE = 32  # texts per forward pass through the model
# The model reads at most 512 tokens. Its tokenizer config doesn't say so, so truncation=True alone
# truncates to a huge default and long turns crash. Passing the limit explicitly fixes that.
MAX_TOKENS = 512

# The model's label names -> ours. The only place that knows how this model spells its labels.
SENTIMENT_TRANSLATE = {
    "negative": SentimentLabel.NEGATIVE,
    "neutral": SentimentLabel.NEUTRAL,
    "positive": SentimentLabel.POSITIVE,
}


class HuggingFaceSentimentClassifier(TextClassifier[SentimentLabel]):
    """Sentiment from cardiffnlp's Twitter RoBERTa model, run locally."""

    def __init__(self) -> None:
        # Slow: loads the model (downloads it the first time). Build once, at startup.
        self.pipeline = pipeline("text-classification", model=MODEL_NAME)

    def classify(self, texts: list[str]) -> list[Prediction[SentimentLabel]]:
        """Labels every text with one batched model call."""
        if not texts:
            return []

        # Turns longer than MAX_TOKENS are cut instead of crashing the request.
        results = self.pipeline(texts, batch_size=BATCH_SIZE, truncation=True, max_length=MAX_TOKENS)
        return [Prediction(SENTIMENT_TRANSLATE[r["label"]], r["score"]) for r in results]

    @property
    def model_version(self) -> str:
        return MODEL_NAME
