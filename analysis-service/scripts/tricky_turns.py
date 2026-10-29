"""Runs hard sentences through the real models and prints a markdown table of where they fail.

Run from the analysis-service folder (needs the ML libraries):
    python -m scripts.tricky_turns
The results are written up in docs/model-notes.md.
"""

from app.analyzers.empathy import HuggingFaceEmpathyClassifier
from app.analyzers.sentiment import HuggingFaceSentimentClassifier

# (text, what a person would say, what makes it hard). "/" means either label is reasonable.
CUSTOMER_TURNS = [
    ("My order never arrived.", "NEGATIVE", "baseline"),
    ("I'm not angry.", "NEUTRAL/POSITIVE", "negation"),
    ("I'm not happy with this at all.", "NEGATIVE", "negated positive word"),
    ("Not bad, actually.", "POSITIVE", "double negative"),
    ("It's been two weeks!", "NEGATIVE", "frustration without negative words"),
    ("Great, another two weeks of waiting.", "NEGATIVE", "sarcasm"),
    ("Oh wonderful, the app crashed again.", "NEGATIVE", "sarcasm"),
    ("Thanks for nothing.", "NEGATIVE", "sarcastic thanks"),
    ("Fine.", "NEUTRAL", "one word"),
    ("I guess that works.", "NEUTRAL", "hedged"),
    ("um so yeah i was like calling about the uh the bill", "NEUTRAL", "speech-to-text style"),
]

AGENT_TURNS = [
    ("I'm so sorry about that, let me fix it right away.", "EMPATHETIC", "baseline"),
    ("I understand how frustrating that must be.", "EMPATHETIC", "acknowledges feelings"),
    ("We apologize for any inconvenience this may have caused.", "EMPATHETIC/NEUTRAL", "scripted apology"),
    ("I'm sorry, but that's our policy.", "UNHELPFUL", "apology + refusal"),
    ("I hear you. I can't refund it, but I can offer a voucher.", "EMPATHETIC", "refusal + alternative"),
    ("As I already said, you need to restart the app.", "UNHELPFUL", "passive-aggressive"),
    ("Calm down, sir.", "UNHELPFUL", "dismissive"),
    ("Let me check that for you.", "NEUTRAL/EMPATHETIC", "helpful, no emotion"),
    ("Is there anything else I can help you with?", "NEUTRAL/EMPATHETIC", "scripted closing"),
    ("Okay.", "NEUTRAL", "one word"),
]


def table(rows, preds) -> str:
    lines = ["| Turn | Model | Expected | Hard because |", "|---|---|---|---|"]
    for (text, expected, why), p in zip(rows, preds):
        ok = "✓" if p.label.value in expected.split("/") else "✗"
        lines.append(f"| {text} | {ok} {p.label.value} {p.confidence:.2f} | {expected} | {why} |")
    return "\n".join(lines)


def main() -> None:
    sentiment = HuggingFaceSentimentClassifier()
    empathy = HuggingFaceEmpathyClassifier()

    print("## Customer turns: sentiment\n")
    print(table(CUSTOMER_TURNS, sentiment.classify([text for text, _, _ in CUSTOMER_TURNS])))

    print("\n## Agent turns: empathy\n")
    print(table(AGENT_TURNS, empathy.classify([text for text, _, _ in AGENT_TURNS])))

    print("\n## Agent turns through the sentiment model (why empathy needs its own model)\n")
    for (text, _, _), p in zip(AGENT_TURNS[:4], sentiment.classify([t for t, _, _ in AGENT_TURNS[:4]])):
        print(f"- {text} → {p.label.value} {p.confidence:.2f}")


if __name__ == "__main__":
    main()
