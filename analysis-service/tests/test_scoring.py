"""The scoring rules, checked with hand-made numbers. No classifiers involved."""

from app.analyzers import scoring
from app.analyzers.base import Prediction
from app.schemas import EmpathyLabel, SentimentLabel


class TestScore:
    def test_negative_label_is_negative_confidence(self):
        assert scoring.score(Prediction(SentimentLabel.NEGATIVE, 0.9), scoring.SENTIMENT_VALUES) == -0.9

    def test_positive_label_is_positive_confidence(self):
        assert scoring.score(Prediction(EmpathyLabel.EMPATHETIC, 0.8), scoring.EMPATHY_VALUES) == 0.8

    def test_neutral_is_zero_whatever_the_confidence(self):
        assert scoring.score(Prediction(SentimentLabel.NEUTRAL, 0.6), scoring.SENTIMENT_VALUES) == 0


class TestCustomerSentiment:
    def test_no_customer_turns_is_none(self):
        assert scoring.customer_sentiment([]) is None

    def test_worked_example_last_turn_counts_double(self):
        # (-0.9 * 1 + 0.9 * 2) / (1 + 2) = 0.3
        assert scoring.customer_sentiment([-0.9, 0.9]) == 30

    def test_single_turn_is_its_own_score(self):
        assert scoring.customer_sentiment([0.5]) == 50

    def test_last_15_percent_of_ten_turns_is_two_turns(self):
        # ceil(10 * 0.15) = 2 turns count double: (1 + 1) * 2 / (8 + 2 * 2) = 0.333
        assert scoring.customer_sentiment([0] * 8 + [1, 1]) == 33

    def test_stays_in_range(self):
        assert scoring.customer_sentiment([-1, -1, -1]) == -100
        assert scoring.customer_sentiment([1, 1, 1]) == 100


class TestSentimentTrend:
    def test_needs_two_customer_turns(self):
        assert scoring.sentiment_trend([]) is None
        assert scoring.sentiment_trend([0.5]) is None

    def test_worked_example_call_turned_around(self):
        # (0.9 - (-0.9)) * 100 / 2
        assert scoring.sentiment_trend([-0.9, 0.9]) == 90

    def test_odd_count_middle_turn_goes_to_second_half(self):
        # first half [-1], second half [0, 1] -> (0.5 - (-1)) * 100 / 2
        assert scoring.sentiment_trend([-1, 0, 1]) == 75

    def test_flat_call_has_no_trend(self):
        assert scoring.sentiment_trend([0.5, 0.5, 0.5, 0.5]) == 0

    def test_worse_call_is_negative(self):
        assert scoring.sentiment_trend([1, -1]) == -100


class TestAgentEmpathy:
    def test_no_agent_turns_is_none(self):
        assert scoring.agent_empathy([]) is None

    def test_plain_average(self):
        assert scoring.agent_empathy([1, 0.5]) == 75

    def test_no_extra_weight_at_the_end(self):
        assert scoring.agent_empathy([0.9, -0.9]) == 0
