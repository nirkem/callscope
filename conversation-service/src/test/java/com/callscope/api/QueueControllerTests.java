package com.callscope.api;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.callscope.CallscopeProperties;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

/** GET /api/queues with a fake SQS client, so no queue has to run. */
class QueueControllerTests {

	private final SqsAsyncClient sqs = mock(SqsAsyncClient.class);

	private final CallscopeProperties properties = new CallscopeProperties("main-queue", "dead-queue", null);

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new QueueController(sqs, properties)).build();

	@Test
	void countsTheQueueAndTheDeadLetterQueue() throws Exception {
		givenQueue("main-queue", 3, 2);
		givenQueue("dead-queue", 1, 0);

		mvc.perform(get("/api/queues"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.queue.name").value("main-queue"))
				.andExpect(jsonPath("$.queue.waiting").value(3))
				.andExpect(jsonPath("$.queue.inFlight").value(2))
				.andExpect(jsonPath("$.deadLetter.name").value("dead-queue"))
				.andExpect(jsonPath("$.deadLetter.waiting").value(1))
				.andExpect(jsonPath("$.deadLetter.inFlight").value(0));
	}

	/** Makes the fake client answer like SQS does for this queue: first its URL, then its two counts. */
	private void givenQueue(String name, int waiting, int inFlight) {
		String url = "http://sqs.test/000000000000/" + name;

		when(sqs.getQueueUrl(GetQueueUrlRequest.builder().queueName(name).build()))
				.thenReturn(CompletableFuture.completedFuture(GetQueueUrlResponse.builder().queueUrl(url).build()));

		// Null check: Mockito also calls this matcher with null while the next stub is being set up.
		when(sqs.getQueueAttributes(argThat((GetQueueAttributesRequest request) ->
				request != null && url.equals(request.queueUrl()))))
				.thenReturn(CompletableFuture.completedFuture(GetQueueAttributesResponse.builder()
						.attributes(Map.of(
								QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES, String.valueOf(waiting),
								QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES_NOT_VISIBLE, String.valueOf(inFlight)))
						.build()));
	}

}
