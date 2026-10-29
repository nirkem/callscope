package com.callscope.client;

import java.util.List;

import com.callscope.messaging.ConversationCompletedEvent.Speaker;

/** What POST /v1/analyze returns. Mirrors AnalyzeResponse in schemas.py; null where Python sends null. */
public record AnalyzeResponse(
		String conversationId,
		ModelVersions modelVersions,
		List<TurnResult> turns,
		Summary summary) {

	public record ModelVersions(String sentiment, String empathy) {
	}

	public record TurnResult(int index, Speaker speaker, SentimentLabel sentiment, EmpathyLabel empathy,
			double confidence) {
	}

	public record Summary(Integer customerSentiment, Integer sentimentTrend, Integer agentEmpathy) {
	}

	public enum SentimentLabel {
		POSITIVE, NEUTRAL, NEGATIVE
	}

	public enum EmpathyLabel {
		EMPATHETIC, NEUTRAL, UNHELPFUL
	}

}
