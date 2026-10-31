package com.callscope.domain;

import java.util.List;
import java.util.Optional;

/**
 * Where conversation records live. The rest of the service talks only to this interface,
 * so the in-memory map can later be replaced by DynamoDB without changing any caller.
 */
public interface ConversationStore {

	/** Stores the record under its conversationId, replacing any older record for that id. */
	void save(ConversationRecord record);

	/**
	 * The record for this id, or Optional.empty() if we've never seen it. Optional instead of null:
	 * the return type itself says "may be missing", so callers can't forget that case.
	 */
	Optional<ConversationRecord> find(String conversationId);

	/**
	 * Every record in the store, in no particular order. A snapshot: records saved after the call
	 * don't show up in the returned list.
	 */
	List<ConversationRecord> findAll();

}
