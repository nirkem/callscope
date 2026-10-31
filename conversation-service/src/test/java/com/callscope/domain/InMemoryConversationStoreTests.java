package com.callscope.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.junit.jupiter.api.Test;

/** The in-memory store on its own: a plain object, no Spring. */
class InMemoryConversationStoreTests {

	private final InMemoryConversationStore store = new InMemoryConversationStore();

	@Test
	void findAllIsEmptyAtFirst() {
		assertThat(store.findAll()).isEmpty();
	}

	@Test
	void findAllHasTheNewestRecordOfEachConversation() {
		store.save(ConversationRecord.pending("c-1"));
		store.save(ConversationRecord.failed("c-1", "timeout"));   // replaces c-1's PENDING record
		store.save(ConversationRecord.pending("c-2"));

		assertThat(store.findAll())
				.extracting(ConversationRecord::conversationId, ConversationRecord::status)
				.containsExactlyInAnyOrder(
						tuple("c-1", ConversationStatus.FAILED),
						tuple("c-2", ConversationStatus.PENDING));
	}

	@Test
	void findAllIsASnapshotThatCantBeChanged() {
		store.save(ConversationRecord.pending("c-1"));
		List<ConversationRecord> before = store.findAll();

		store.save(ConversationRecord.pending("c-2"));

		assertThat(before).hasSize(1);
		assertThatThrownBy(() -> before.add(ConversationRecord.pending("c-3")))
				.isInstanceOf(UnsupportedOperationException.class);
	}

}
