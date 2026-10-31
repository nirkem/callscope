package com.callscope.messaging;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.springframework.stereotype.Component;

import com.callscope.service.ConversationProcessor;

import io.awspring.cloud.sqs.annotation.SqsListener;

/**
 * Receives conversation.completed events from SQS and hands each one to the processor.
 *
 * <p>How SQS decides what happens to a message: if {@link #onMessage} returns normally, Spring
 * Cloud AWS deletes the message ("done"). If it throws, the message stays in the queue and comes
 * back after the visibility timeout; after 3 tries it moves to the dead-letter queue.
 */
@Component
public class ConversationEventListener {

	private final ConversationProcessor processor;

	// Runs the @NotBlank / @NotEmpty / @Size rules declared on ConversationCompletedEvent.
	private final Validator validator;

	public ConversationEventListener(ConversationProcessor processor, Validator validator) {
		this.processor = processor;
		this.validator = validator;
	}

	/**
	 * Called once per message, with the JSON already converted to the record. The queue name comes
	 * from callscope.queue-name in application.yml.
	 *
	 * @throws ConstraintViolationException if the event breaks the contract (e.g. no turns). It can never
	 *         succeed, so it goes through the retries to the dead-letter queue.
	 */
	@SqsListener("${callscope.queue-name}")
	public void onMessage(ConversationCompletedEvent event) {
		// Assert the annotations on the ConversationCompletedEvent record.
		Set<ConstraintViolation<ConversationCompletedEvent>> violations = validator.validate(event);
		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}

		processor.process(event);
	}

}
