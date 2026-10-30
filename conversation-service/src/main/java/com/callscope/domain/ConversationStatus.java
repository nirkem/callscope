package com.callscope.domain;

/** Where a conversation is in processing. */
public enum ConversationStatus {

	/** Event received; analysis-service is being called. */
	PENDING,

	/** The analysis result is stored. */
	DONE,

	/** Analysis failed. SQS retries the message; after 3 tries it goes to the dead-letter queue. */
	FAILED

}
