package com.callscope.api;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.callscope.CallscopeProperties;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/**
 * {@code GET /api/queues}: how many messages the queue and the dead-letter queue hold right now.
 * Only the dashboard page uses it.
 *
 * <p>SQS can't list the messages it holds, only count them, in two groups:
 * <ul>
 * <li>waiting: sent, and nobody has received it yet;
 * <li>in flight: received, but not deleted yet. Either a listener is working on it right now, or it
 * failed and stays hidden until the visibility timeout (20 s) runs out; then it's waiting again.
 * </ul>
 */
@RestController
public class QueueController {

	// SQS's own names for the two counts.
	private static final QueueAttributeName WAITING = QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES;
	private static final QueueAttributeName IN_FLIGHT = QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE;

	// The SQS client Spring Cloud AWS already built for the listener, so it uses the same endpoint.
	private final SqsAsyncClient sqs;

	private final CallscopeProperties properties;

	public QueueController(SqsAsyncClient sqs, CallscopeProperties properties) {
		this.sqs = sqs;
		this.properties = properties;
	}

	@GetMapping("/api/queues")
	public QueueStats queues() {
		return new QueueStats(count(properties.queueName()), count(properties.deadLetterQueueName()));
	}

	private QueueCounts count(String queueName) {
		// SQS calls take the queue's URL, not its name, so look the URL up first.
		// The client is async: each call returns a CompletableFuture, and join() waits for the answer.
		String url = sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(queueName).build()).join().queueUrl();

		Map<QueueAttributeName, String> attributes = sqs.getQueueAttributes(GetQueueAttributesRequest.builder()
				.queueUrl(url)
				.attributeNames(WAITING, IN_FLIGHT)
				.build())
			.join()
			.attributes();

		return new QueueCounts(queueName, Integer.parseInt(attributes.get(WAITING)),
				Integer.parseInt(attributes.get(IN_FLIGHT)));
	}

	/** The JSON body: {@code {"queue": {...}, "deadLetter": {...}}}. */
	public record QueueStats(QueueCounts queue, QueueCounts deadLetter) {
	}

	/** One queue's counts. SQS calls them approximate: exact enough for a dashboard. */
	public record QueueCounts(String name, int waiting, int inFlight) {
	}

}
