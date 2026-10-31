package com.callscope.api;

import java.util.List;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.callscope.domain.ConversationRecord;
import com.callscope.domain.ConversationStore;

/**
 * Read-only HTTP API over the store: one conversation's status and result, or all of them.
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
	 * {@code GET /api/conversations}: every record, as a JSON list. The dashboard page
	 * (src/main/resources/static/index.html) calls it twice a second.
	 *
	 * <p>Always 200, with {@code []} when nothing has arrived yet: an empty list is a normal answer,
	 * not a missing resource. So unlike {@link #get}, there's no 404 to choose, and the method can
	 * return the list itself instead of a ResponseEntity; Spring sends it as JSON with status 200.
	 */
	@GetMapping
	public List<ConversationRecord> list() {
		return store.findAll();
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
