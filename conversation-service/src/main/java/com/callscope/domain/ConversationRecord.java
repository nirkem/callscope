package com.callscope.domain;

import java.time.Instant;

import com.callscope.client.AnalyzeResponse;

/**
 * Everything conversation-service knows about one conversation.
 *
 * <p>A record is Java's built-in "data only" class. From the header below, Java generates:
 * a constructor taking all five fields in order, a read-only accessor per field
 * ({@code record.status()}, without "get"), and equals/hashCode/toString.
 *
 * <p>Records are immutable: there are no setters, and the fields can't change after creation.
 * So a status change (PENDING to DONE) never edits a record; it creates a new one that replaces
 * the old one in the store. That matters because several SQS messages are processed on different
 * threads at the same time: a thread reading a record can never see it half-updated by another.
 */
public record ConversationRecord(
		String conversationId, // the key; what GET /api/conversations/{id} looks up
		ConversationStatus status,
		AnalyzeResponse result, // null until DONE
		String error, // why it failed; null unless FAILED
		Instant updatedAt // when the status last changed: shows how long something has been PENDING
) {

	/*
	 * Factory methods: named shortcuts for the three valid states.
	 *
	 * The constructor above accepts any combination, including contradictions such as DONE with no
	 * result, or FAILED with a result. Callers use these three methods instead, so every record
	 * they create is one of the valid shapes, and the code reads like the process it describes:
	 *
	 *     store.save(ConversationRecord.pending(id));
	 *     store.save(ConversationRecord.done(id, result));
	 *
	 * "static" means the method belongs to the type, not to an existing record: it is called as
	 * ConversationRecord.pending(id), before any record exists. Each one just calls the generated
	 * constructor with the right values, in the order of the header: id, status, result, error, time.
	 */

	/** Event received; analysis-service is being called. No result or error yet. */
	public static ConversationRecord pending(String conversationId) {
		return new ConversationRecord(conversationId, ConversationStatus.PENDING, null, null, Instant.now());
	}

	/** Analysis finished: carries the result from analysis-service. */
	public static ConversationRecord done(String conversationId, AnalyzeResponse result) {
		return new ConversationRecord(conversationId, ConversationStatus.DONE, result, null, Instant.now());
	}

	/** Analysis failed: carries the reason, so GET can show why instead of just "FAILED". */
	public static ConversationRecord failed(String conversationId, String error) {
		return new ConversationRecord(conversationId, ConversationStatus.FAILED, null, error, Instant.now());
	}

}
