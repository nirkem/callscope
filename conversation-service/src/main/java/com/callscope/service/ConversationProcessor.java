package com.callscope.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;

import com.callscope.client.AnalysisClient;
import com.callscope.client.AnalyzeRequest;
import com.callscope.client.AnalyzeResponse;
import com.callscope.domain.ConversationRecord;
import com.callscope.domain.ConversationStatus;
import com.callscope.domain.ConversationStore;
import com.callscope.messaging.ConversationCompletedEvent;

/**
 * The flow for one conversation: skip duplicates, mark PENDING, call analysis-service,
 * then mark DONE or FAILED. Knows nothing about SQS or HTTP; the listener calls it.
 */
@Service
public class ConversationProcessor {

	private static final Logger log = LoggerFactory.getLogger(ConversationProcessor.class);

	private final AnalysisClient analysisClient;
	private final ConversationStore conversationStore;

	public ConversationProcessor(AnalysisClient analysisClient, ConversationStore conversationStore) {
		this.analysisClient = analysisClient;
		this.conversationStore = conversationStore;
	}

	public void process(ConversationCompletedEvent event) {
		String id = event.conversationId();
		Optional<ConversationRecord> record = conversationStore.find(id);

		// SQS can deliver a message twice. Skip finished work.
		if (record.isPresent() && record.get().status() == ConversationStatus.DONE) {
			log.info("Skipping {}: already DONE", id);
			return;
		}

		conversationStore.save(ConversationRecord.pending(id));
		try {
			AnalyzeResponse response = analysisClient.analyze(new AnalyzeRequest(id, event.turns()));
			conversationStore.save(ConversationRecord.done(id, response));
			log.info("Analyzed {}", id);
		}
		catch (RuntimeException e) {
			String reason = rootCauseMessage(e);
			conversationStore.save(ConversationRecord.failed(id, reason));
			log.warn("Analysis of {} failed, SQS will retry: {}", id, reason);
			// Rethrow so the listener doesn't confirm the message: SQS retries it, and after
			// 3 failed tries moves it to the dead-letter queue. Swallowing it would lose it.
			throw e;
		}
	}

	/**
	 * Why it failed, taken from the innermost exception. The outer ones are often wrappers whose
	 * message hides the real problem: a read timeout arrives as "Error while extracting response ...
	 * content type [application/octet-stream]", and only its innermost cause says
	 * "SocketTimeoutException: Read timed out".
	 */
	private static String rootCauseMessage(Exception e) {
		Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
		String type = cause.getClass().getSimpleName();
		return cause.getMessage() == null ? type : type + ": " + cause.getMessage();
	}

}
