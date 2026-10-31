package com.callscope.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.callscope.CallscopeProperties;
import com.callscope.messaging.ConversationCompletedEvent;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.json.JsonMapper;

/** POST /api/demo/batch with a fake SQS client: checks what would be sent to the queue. */
class DemoControllerTests {

	private final SqsAsyncClient sqs = mock(SqsAsyncClient.class);

	private final JsonMapper json = JsonMapper.builder().build();

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
			new DemoController(sqs, new CallscopeProperties("main-queue", "dead-queue", null), json)).build();

	@BeforeEach
	void fakeQueue() {
		when(sqs.getQueueUrl(any(GetQueueUrlRequest.class))).thenReturn(CompletableFuture.completedFuture(
				GetQueueUrlResponse.builder().queueUrl("http://sqs.test/000000000000/main-queue").build()));
		when(sqs.sendMessage(any(SendMessageRequest.class)))
			.thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));
	}

	@Test
	void sendsValidConversationsToTheQueue() throws Exception {
		mvc.perform(post("/api/demo/batch").param("count", "3"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sent").value(3));

		for (ConversationCompletedEvent event : sentEvents(3)) {
			assertThat(event.conversationId()).startsWith("c-");
			assertThat(event.turns()).hasSizeBetween(3, 5);
		}
	}

	@Test
	void duplicateRepeatsTheLastEventAndInvalidAddsOneWithNoTurns() throws Exception {
		mvc.perform(post("/api/demo/batch").param("count", "2").param("duplicate", "true").param("invalid", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.sent").value(4));

		List<ConversationCompletedEvent> events = sentEvents(4);
		assertThat(events.get(2)).isEqualTo(events.get(1));
		assertThat(events.get(3).turns()).isEmpty();
	}

	@Test
	void countIsKeptBetween1And50() throws Exception {
		mvc.perform(post("/api/demo/batch").param("count", "1000"))
				.andExpect(jsonPath("$.sent").value(50));
	}

	/** The message bodies the fake client was asked to send, read back as events. */
	private List<ConversationCompletedEvent> sentEvents(int expected) {
		ArgumentCaptor<SendMessageRequest> sent = ArgumentCaptor.forClass(SendMessageRequest.class);
		verify(sqs, times(expected)).sendMessage(sent.capture());
		return sent.getAllValues().stream()
			.map(request -> json.readValue(request.messageBody(), ConversationCompletedEvent.class))
			.toList();
	}

}
