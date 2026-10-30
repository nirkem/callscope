package com.callscope.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;

import org.junit.jupiter.api.Test;

import com.callscope.messaging.ConversationCompletedEvent.Speaker;
import com.callscope.messaging.ConversationCompletedEvent.Turn;
import com.callscope.service.ConversationProcessor;

/** The listener with a real validator and a mock processor. No queue needed. */
class ConversationEventListenerTests {

	private final ConversationProcessor processor = mock(ConversationProcessor.class);

	private final ConversationEventListener listener = new ConversationEventListener(processor,
			Validation.buildDefaultValidatorFactory().getValidator());

	@Test
	void validEventIsProcessed() {
		ConversationCompletedEvent event = event(List.of(new Turn(Speaker.CUSTOMER, "hi")));

		listener.onMessage(event);

		verify(processor).process(event);
	}

	@Test
	void eventWithNoTurnsIsRejectedBeforeProcessing() {
		// What the simulator's --invalid sends. Throwing sends it through the retries to the DLQ.
		ConversationCompletedEvent event = event(List.of());

		assertThatThrownBy(() -> listener.onMessage(event))
				.isInstanceOf(ConstraintViolationException.class)
				.hasMessageContaining("turns");
		verify(processor, never()).process(any());
	}

	private static ConversationCompletedEvent event(List<Turn> turns) {
		return new ConversationCompletedEvent("e-1", "c-1", Instant.parse("2026-10-30T10:00:00Z"), turns);
	}

}
