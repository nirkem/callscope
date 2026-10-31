package com.callscope.demo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.callscope.CallscopeProperties;
import com.callscope.messaging.ConversationCompletedEvent;
import com.callscope.messaging.ConversationCompletedEvent.Speaker;
import com.callscope.messaging.ConversationCompletedEvent.Turn;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code POST /api/demo/batch}: the dashboard's "Simulate a batch" button. A Java copy of
 * simulator/publish_events.py, so a demo needs only a browser.
 *
 * <p>It sends the events to the queue, like the real platform would, instead of calling the
 * processor directly: every event still goes the whole way (queue, listener, analysis, store).
 *
 * <p>Demo only, so it exists only when {@code callscope.demo.enabled=true} (docker-compose turns it on).
 * Otherwise Spring never creates this class, and the path is a 404.
 */
@RestController
@ConditionalOnProperty(name = "callscope.demo.enabled", havingValue = "true")
public class DemoController {

	// The same sentence pools as simulator/publish_events.py.
	private static final List<String> CUSTOMER_OPENERS = List.of(
			"My order never arrived.",
			"I was charged twice this month.",
			"The app keeps crashing when I log in.",
			"I'm really angry, nobody answered my email.",
			"Hi, I'd like to change my delivery address.");
	private static final List<String> AGENT_REPLIES = List.of(
			"I'm so sorry about that, let me check right away.",
			"I understand how frustrating that is.",
			"That's our policy, I can't change it.",
			"Can you confirm the email on the account?",
			"Let me look into it.");
	private static final List<String> CUSTOMER_FOLLOWUPS = List.of(
			"It's been two weeks!",
			"Sure, it's the one ending in 42.",
			"This is the third time I'm calling.",
			"Okay.");
	private static final List<String> CUSTOMER_CLOSINGS = List.of(
			"Thanks, that's great.",
			"Great, thanks for the help!",
			"This never works. I'm cancelling.",
			"Okay, bye.");

	private static final int MAX_COUNT = 50;

	private final SqsAsyncClient sqs;
	private final CallscopeProperties properties;
	private final JsonMapper json;

	public DemoController(SqsAsyncClient sqs, CallscopeProperties properties, JsonMapper json) {
		this.sqs = sqs;
		this.properties = properties;
		this.json = json;
	}

	/**
	 * Sends {@code count} random conversations (1 to 50). {@code duplicate} sends the last one a second
	 * time (tests idempotency); {@code invalid} adds an event with no turns (ends in the dead-letter queue).
	 */
	@PostMapping("/api/demo/batch")
	public BatchSent batch(
			@RequestParam(defaultValue = "10") int count,
			@RequestParam(defaultValue = "false") boolean duplicate,
			@RequestParam(defaultValue = "false") boolean invalid) {
		List<ConversationCompletedEvent> events = new ArrayList<>();
		for (int i = 0; i < Math.clamp(count, 1, MAX_COUNT); i++) {
			events.add(randomEvent());
		}
		if (duplicate) {
			events.add(events.getLast());
		}
		if (invalid) {
			ConversationCompletedEvent broken = randomEvent();
			events.add(new ConversationCompletedEvent(broken.eventId(), broken.conversationId(), broken.completedAt(),
					List.of()));
		}

		String url = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(properties.queueName()).build())
			.join()
			.queueUrl();
		for (ConversationCompletedEvent event : events) {
			sqs.sendMessage(SendMessageRequest.builder().queueUrl(url).messageBody(json.writeValueAsString(event)).build())
				.join();
		}
		return new BatchSent(events.size(), events.stream().map(ConversationCompletedEvent::conversationId).toList());
	}

	/** One conversation in the shape of contracts/examples/conversation-completed.json. */
	private static ConversationCompletedEvent randomEvent() {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		List<Turn> turns = new ArrayList<>();
		turns.add(new Turn(Speaker.CUSTOMER, pick(CUSTOMER_OPENERS)));
		turns.add(new Turn(Speaker.AGENT, pick(AGENT_REPLIES)));
		if (random.nextDouble() < 0.6) {   // some conversations go back and forth longer
			turns.add(new Turn(Speaker.CUSTOMER, pick(CUSTOMER_FOLLOWUPS)));
			turns.add(new Turn(Speaker.AGENT, pick(AGENT_REPLIES)));
		}
		turns.add(new Turn(Speaker.CUSTOMER, pick(CUSTOMER_CLOSINGS)));

		String conversationId = "c-" + UUID.randomUUID().toString().substring(0, 8);
		return new ConversationCompletedEvent(UUID.randomUUID().toString(), conversationId, Instant.now(), turns);
	}

	private static String pick(List<String> options) {
		return options.get(ThreadLocalRandom.current().nextInt(options.size()));
	}

	/** The JSON answer: how many messages went to the queue, and their conversation ids. */
	public record BatchSent(int sent, List<String> conversationIds) {
	}

}
