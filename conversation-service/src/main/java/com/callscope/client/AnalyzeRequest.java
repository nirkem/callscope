package com.callscope.client;

import java.util.List;

import com.callscope.messaging.ConversationCompletedEvent.Turn;

/** Body of POST /v1/analyze on analysis-service. Mirrors AnalyzeRequest in schemas.py. */
public record AnalyzeRequest(String conversationId, List<Turn> turns) {
}
