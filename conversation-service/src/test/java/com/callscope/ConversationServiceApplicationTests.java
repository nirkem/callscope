package com.callscope;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.callscope.api.QueueController;
import com.callscope.messaging.ConversationEventListener;
import com.callscope.service.ConversationProcessor;

import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Starts the whole application. SQS is switched off: otherwise the listener connects to the queue
 * at startup, and this test would fail whenever the queue isn't running (as in CI).
 */
@SpringBootTest(properties = "spring.cloud.aws.sqs.enabled=false")
class ConversationServiceApplicationTests {

	// With SQS off, Spring Cloud AWS builds no SQS client either, and QueueController needs one.
	@MockitoBean
	private SqsAsyncClient sqsAsyncClient;

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoadsWithAllComponentsWired() {
		assertThat(context.getBean(ConversationEventListener.class)).isNotNull();
		assertThat(context.getBean(ConversationProcessor.class)).isNotNull();
		assertThat(context.getBean(QueueController.class)).isNotNull();
	}

}
