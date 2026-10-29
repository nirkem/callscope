package com.callscope;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import com.callscope.messaging.ConversationCompletedEvent;

import tools.jackson.databind.json.JsonMapper;

/**
 * The shared example messages must load into this service's records.
 * analysis-service runs the same check on its side, so a renamed field fails on both.
 */
class ContractTests {

	private static final Path EXAMPLES = Path.of("..", "contracts", "examples");

	private final JsonMapper json = JsonMapper.builder().build();

	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void conversationCompletedExampleLoadsAndIsValid() throws Exception {
		String body = Files.readString(EXAMPLES.resolve("conversation-completed.json"));

		ConversationCompletedEvent event = json.readValue(body, ConversationCompletedEvent.class);

		assertThat(event.conversationId()).isEqualTo("c-123");
		assertThat(event.turns()).hasSize(2);
		assertThat(event.turns().get(0).speaker()).isEqualTo(ConversationCompletedEvent.Speaker.CUSTOMER);
		assertThat(validator.validate(event)).isEmpty();
	}

	@Test
	void eventWithNoTurnsIsInvalid() throws Exception {
		String body = """
				{"eventId": "e-1", "conversationId": "c-1", "completedAt": "2026-10-27T10:00:00Z", "turns": []}
				""";

		ConversationCompletedEvent event = json.readValue(body, ConversationCompletedEvent.class);

		assertThat(validator.validate(event)).isNotEmpty();
	}

}
