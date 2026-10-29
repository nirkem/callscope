"""Stands in for the contact-center platform: publishes conversation.completed events to SQS.

Examples (from the callscope folder, with the queue running):
    python simulator/publish_events.py                 # 5 random conversations
    python simulator/publish_events.py --count 20 --seed 7
    python simulator/publish_events.py --duplicate     # also resend the last event: tests idempotency
    python simulator/publish_events.py --invalid       # also send a broken event: ends in the dead-letter queue
"""

import argparse
import json
import os
import random
import uuid
from datetime import datetime, timezone

import boto3

CUSTOMER_OPENERS = [
    "My order never arrived.",
    "I was charged twice this month.",
    "The app keeps crashing when I log in.",
    "I'm really angry, nobody answered my email.",
    "Hi, I'd like to change my delivery address.",
]
AGENT_REPLIES = [
    "I'm so sorry about that, let me check right away.",
    "I understand how frustrating that is.",
    "That's our policy, I can't change it.",
    "Can you confirm the email on the account?",
    "Let me look into it.",
]
CUSTOMER_FOLLOWUPS = [
    "It's been two weeks!",
    "Sure, it's the one ending in 42.",
    "This is the third time I'm calling.",
    "Okay.",
]
CUSTOMER_CLOSINGS = [
    "Thanks, that's great.",
    "Great, thanks for the help!",
    "This never works. I'm cancelling.",
    "Okay, bye.",
]


def make_event(rng: random.Random) -> dict:
    """One event in the shape of contracts/examples/conversation-completed.json."""
    turns = [
        {"speaker": "CUSTOMER", "text": rng.choice(CUSTOMER_OPENERS)},
        {"speaker": "AGENT", "text": rng.choice(AGENT_REPLIES)},
    ]
    if rng.random() < 0.6:  # some conversations go back and forth longer
        turns.append({"speaker": "CUSTOMER", "text": rng.choice(CUSTOMER_FOLLOWUPS)})
        turns.append({"speaker": "AGENT", "text": rng.choice(AGENT_REPLIES)})
    turns.append({"speaker": "CUSTOMER", "text": rng.choice(CUSTOMER_CLOSINGS)})

    return {
        "eventId": str(uuid.uuid4()),
        "conversationId": f"c-{uuid.uuid4().hex[:8]}",
        "completedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "turns": turns,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--count", type=int, default=5, help="how many conversations to send")
    parser.add_argument("--seed", type=int, help="fixed seed, for the same conversations every run")
    parser.add_argument("--duplicate", action="store_true", help="send the last event a second time")
    parser.add_argument("--invalid", action="store_true", help="also send an event with no turns")
    parser.add_argument("--endpoint", default=os.environ.get("SQS_ENDPOINT", "http://localhost:9324"))
    parser.add_argument("--queue", default="conversation-completed")
    args = parser.parse_args()

    # Local queue: the credentials are placeholders. Against real SQS, leave out endpoint_url and the keys.
    sqs = boto3.client(
        "sqs",
        endpoint_url=args.endpoint,
        region_name="us-east-1",
        aws_access_key_id="local",
        aws_secret_access_key="local",
    )
    queue_url = sqs.get_queue_url(QueueName=args.queue)["QueueUrl"]
    rng = random.Random(args.seed)

    events = [make_event(rng) for _ in range(args.count)]
    if args.duplicate and events:
        events.append(events[-1])
    if args.invalid:
        events.append({**make_event(rng), "turns": []})

    for event in events:
        sqs.send_message(QueueUrl=queue_url, MessageBody=json.dumps(event))
        print(f"sent {event['conversationId']}  ({len(event['turns'])} turns)")
    print(f"{len(events)} events sent to {queue_url}")


if __name__ == "__main__":
    main()
