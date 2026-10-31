package com.callscope.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import com.callscope.client.AnalysisClient;
import com.callscope.client.AnalyzeResponse;
import com.callscope.domain.ConversationRecord;
import com.callscope.domain.ConversationStatus;
import com.callscope.domain.InMemoryConversationStore;
import com.callscope.messaging.ConversationCompletedEvent;
import com.callscope.messaging.ConversationCompletedEvent.Speaker;
import com.callscope.messaging.ConversationCompletedEvent.Turn;

/**
 * The processor with a real in-memory store and a mock AnalysisClient: a stand-in object
 * (Mockito) that returns or throws whatever each test tells it to, with no HTTP involved.
 */
class ConversationProcessorTests {

	private static final ConversationCompletedEvent EVENT = new ConversationCompletedEvent(
			"e-1", "c-1", Instant.parse("2026-10-30T10:00:00Z"), List.of(new Turn(Speaker.CUSTOMER, "hi")));

	private static final AnalyzeResponse RESULT = new AnalyzeResponse("c-1", null, List.of(), null);

	private final AnalysisClient client = mock(AnalysisClient.class);

	private final InMemoryConversationStore store = new InMemoryConversationStore();

	private final ConversationProcessor processor = new ConversationProcessor(client, store);

	@Test
	void newConversationEndsDoneWithTheResult() {
		when(client.analyze(any())).thenReturn(RESULT);

		processor.process(EVENT);

		ConversationRecord record = store.find("c-1").orElseThrow();
		assertThat(record.status()).isEqualTo(ConversationStatus.DONE);
		assertThat(record.result()).isEqualTo(RESULT);
	}

	@Test
	void failureIsStoredAndRethrownSoSqsRetries() {
		when(client.analyze(any())).thenThrow(new ResourceAccessException("Connection refused"));

		assertThatThrownBy(() -> processor.process(EVENT)).isInstanceOf(ResourceAccessException.class);

		ConversationRecord record = store.find("c-1").orElseThrow();
		assertThat(record.status()).isEqualTo(ConversationStatus.FAILED);
		assertThat(record.error()).contains("Connection refused");
	}

	@Test
	void failureStoresTheRootCauseNotTheWrapper() {
		// The shape of a real read timeout: RestClient wraps the socket's exception in its own.
		when(client.analyze(any())).thenThrow(new RestClientException(
				"Error while extracting response for type [AnalyzeResponse] and content type [application/octet-stream]",
				new SocketTimeoutException("Read timed out")));

		assertThatThrownBy(() -> processor.process(EVENT)).isInstanceOf(RestClientException.class);

		assertThat(store.find("c-1").orElseThrow().error()).isEqualTo("SocketTimeoutException: Read timed out");
	}

	@Test
	void retryAfterAFailureRunsAgainAndEndsDone() {
		store.save(ConversationRecord.failed("c-1", "earlier failure"));
		when(client.analyze(any())).thenReturn(RESULT);

		processor.process(EVENT);

		assertThat(store.find("c-1").orElseThrow().status()).isEqualTo(ConversationStatus.DONE);
		verify(client, times(1)).analyze(any());
	}

	@Test
	void duplicateOfADoneConversationIsSkipped() {
		store.save(ConversationRecord.done("c-1", RESULT));

		processor.process(EVENT);

		verify(client, never()).analyze(any());
	}

}
