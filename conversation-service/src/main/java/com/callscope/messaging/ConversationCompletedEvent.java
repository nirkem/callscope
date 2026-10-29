package com.callscope.messaging;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The SQS message published when a conversation ends.
 * Same shape as contracts/examples/conversation-completed.json.
 */
public record ConversationCompletedEvent(
		@NotBlank String eventId,
		@NotBlank String conversationId,
		@NotNull Instant completedAt,
		@NotEmpty @Size(max = 500) List<@Valid @NotNull Turn> turns) {

	public record Turn(@NotNull Speaker speaker, @NotBlank @Size(max = 5000) String text) {
	}

	public enum Speaker {
		CUSTOMER, AGENT
	}

}
