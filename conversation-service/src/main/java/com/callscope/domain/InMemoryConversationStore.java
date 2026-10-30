package com.callscope.domain;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Keeps records in a map, in this process's memory. Enough for the demo; everything is lost
 * on restart. Production would use DynamoDB behind the same ConversationStore interface.
 */
@Component
public class InMemoryConversationStore implements ConversationStore {

	// ConcurrentHashMap, not HashMap: SQS messages are handled on several threads at once.
	private final Map<String, ConversationRecord> records = new ConcurrentHashMap<>();

	@Override
	public void save(ConversationRecord record) {
		records.put(record.conversationId(), record);
	}

	@Override
	public Optional<ConversationRecord> find(String conversationId) {
		return Optional.ofNullable(records.get(conversationId));
	}

}
