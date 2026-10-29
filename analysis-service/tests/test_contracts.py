"""Contract tests: the shared example messages must load into this service's models.

If someone renames a field in the examples or in schemas.py, this fails.
"""

import json
from pathlib import Path

from app.schemas import AnalyzeRequest

EXAMPLES = Path(__file__).resolve().parents[2] / "contracts" / "examples"


def test_conversation_completed_event_turns_load_as_an_analyze_request():
    event = json.loads((EXAMPLES / "conversation-completed.json").read_text(encoding="utf-8"))
    # conversation-service forwards the conversation ID and turns, not the SQS eventId.
    request = AnalyzeRequest.model_validate(
        {"conversationId": event["conversationId"], "turns": event["turns"]}
    )
    assert request.conversation_id == event["conversationId"]
    assert len(request.turns) == len(event["turns"])
