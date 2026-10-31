"""ConversationAnalyzer with the keyword fakes, so every label is predictable."""

from app.analyzers.base import Prediction, TextClassifier
from app.analyzers.conversation import ConversationAnalyzer
from app.analyzers.fakes import FakeEmpathyClassifier, FakeSentimentClassifier
from app.schemas import AnalyzeRequest, EmpathyLabel, SentimentLabel, Speaker


def request(*turns: tuple[str, str]) -> AnalyzeRequest:
    # model_validate: built from raw data, the way a JSON body arrives.
    return AnalyzeRequest.model_validate({
        "conversationId": "c-1",
        "turns": [{"speaker": speaker, "text": text} for speaker, text in turns],
    })


def test_results_come_back_in_the_original_order(analyzer):
    response = analyzer.analyze(request(
        ("CUSTOMER", "My order never arrived"),
        ("CUSTOMER", "It's been two weeks!"),
        ("AGENT", "I'm so sorry, let me check"),
        ("CUSTOMER", "Thanks, that's great"),
    ))
    assert [t.index for t in response.turns] == [0, 1, 2, 3]
    assert [t.speaker for t in response.turns] == [Speaker.CUSTOMER, Speaker.CUSTOMER, Speaker.AGENT, Speaker.CUSTOMER]


def test_customer_turns_get_sentiment_and_agent_turns_get_empathy(analyzer):
    response = analyzer.analyze(request(
        ("CUSTOMER", "My order never arrived"),
        ("AGENT", "I'm so sorry, let me check"),
    ))
    customer, agent = response.turns
    assert customer.sentiment == SentimentLabel.NEGATIVE and customer.empathy is None
    assert agent.empathy == EmpathyLabel.EMPATHETIC and agent.sentiment is None


def test_conversation_with_only_an_agent(analyzer):
    response = analyzer.analyze(request(("AGENT", "Hello?")))
    assert response.turns[0].empathy == EmpathyLabel.NEUTRAL
    assert response.summary.customer_sentiment is None
    assert response.summary.sentiment_trend is None


def test_model_versions_come_from_the_classifiers(analyzer):
    response = analyzer.analyze(request(("CUSTOMER", "hi")))
    assert response.model_versions.sentiment == "fake-sentiment-v1"
    assert response.model_versions.empathy == "fake-empathy-v1"


def test_summary_worked_example(analyzer):
    response = analyzer.analyze(request(
        ("CUSTOMER", "My order never arrived"),    # NEGATIVE 0.9 -> -0.9
        ("AGENT", "I'm so sorry, let me check"),   # EMPATHETIC 0.9 -> +0.9
        ("CUSTOMER", "Thanks, that's great"),      # POSITIVE 0.9 -> +0.9, counts double
    ))
    assert response.summary.customer_sentiment == 30
    assert response.summary.sentiment_trend == 90
    assert response.summary.agent_empathy == 90


class CountingClassifier(FakeSentimentClassifier):
    """The sentiment fake, counting how often classify is called."""

    def __init__(self) -> None:
        self.calls = 0

    def classify(self, texts: list[str]) -> list[Prediction[SentimentLabel]]:
        self.calls += 1
        return super().classify(texts)


def test_one_classify_call_per_classifier_however_long_the_conversation():
    sentiment = CountingClassifier()
    analyzer = ConversationAnalyzer(sentiment, FakeEmpathyClassifier())
    analyzer.analyze(request(*[("CUSTOMER", f"message {i}") for i in range(50)]))
    assert sentiment.calls == 1


def test_classifiers_are_interchangeable():
    # Anything that follows the TextClassifier interface can be plugged in.
    assert isinstance(FakeSentimentClassifier(), TextClassifier)
    assert isinstance(FakeEmpathyClassifier(), TextClassifier)
