package com.callscope.api;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.callscope.domain.ConversationRecord;
import com.callscope.domain.ConversationStore;

/**
 * Read-only HTTP API: the status and result of a conversation.
 *
 * <p>{@code @RestController} makes Spring route HTTP requests to this class's methods and turn their
 * return values into JSON. Every path here starts with {@code /api/conversations}.
 */
@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

	private final ConversationStore store;

	public ConversationController(ConversationStore store) {
		this.store = store;
	}

	/**
	 * {@code GET /api/conversations/{id}}: 200 with the record (status, and the result or the error),
	 * or 404 if this conversation was never received. A PENDING record means analysis is still running.
	 */
	@GetMapping("/{id}")
	public ResponseEntity<ConversationRecord> get(@PathVariable String id) {
		Optional<ConversationRecord> record = store.find(id);

		if (record.isPresent()) {
			return ResponseEntity.ok(record.get());
		}
		else {
			return ResponseEntity.notFound().build();
		}
	}

}
