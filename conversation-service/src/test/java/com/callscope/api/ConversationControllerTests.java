package com.callscope.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.callscope.client.AnalyzeResponse;
import com.callscope.client.AnalyzeResponse.Summary;
import com.callscope.domain.ConversationRecord;
import com.callscope.domain.InMemoryConversationStore;

/**
 * The controller over HTTP, without starting a server: MockMvc sends fake requests straight to it
 * and checks the status code and JSON, like FastAPI's TestClient.
 */
class ConversationControllerTests {

	private final InMemoryConversationStore store = new InMemoryConversationStore();

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ConversationController(store)).build();

	@Test
	void unknownConversationIs404() throws Exception {
		mvc.perform(get("/api/conversations/nope")).andExpect(status().isNotFound());
	}

	@Test
	void doneConversationReturnsItsResult() throws Exception {
		store.save(ConversationRecord.done("c-1",
				new AnalyzeResponse("c-1", null, List.of(), new Summary(30, 90, 90))));

		mvc.perform(get("/api/conversations/c-1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DONE"))
				.andExpect(jsonPath("$.result.summary.customerSentiment").value(30));
	}

	@Test
	void failedConversationShowsWhy() throws Exception {
		store.save(ConversationRecord.failed("c-2", "Connection refused"));

		mvc.perform(get("/api/conversations/c-2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("FAILED"))
				.andExpect(jsonPath("$.error").value("Connection refused"));
	}

}
