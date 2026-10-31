package com.callscope.domain;

import java.util.List;
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

	@Override
	public List<ConversationRecord> findAll() {
		// records.values() is a live view of the map: it keeps changing while listener threads save.
		// List.copyOf takes a snapshot (an unmodifiable list), so the caller's list can't change under
		// it while Spring turns it into JSON. Copying during saves is safe because the map is a
		// ConcurrentHashMap: reading it while other threads write never throws, unlike a HashMap.
		return List.copyOf(records.values());
	}

}
