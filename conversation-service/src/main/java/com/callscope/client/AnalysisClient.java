package com.callscope.client;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.callscope.CallscopeProperties;

/** Calls analysis-service (FastAPI): POST /v1/analyze. */
@Component
public class AnalysisClient {

	// Spring's HTTP client: built once, reused for every call (it's thread-safe).
	private final RestClient restClient;

	public AnalysisClient(CallscopeProperties properties) {
		// The timeouts. Without them, a stuck analysis-service would block a listener thread forever.
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(properties.analysisService().connectTimeout());
		requestFactory.setReadTimeout(properties.analysisService().readTimeout());

		this.restClient = RestClient.builder()
				.baseUrl(properties.analysisService().baseUrl())
				.requestFactory(requestFactory)
				.build();
	}

	/**
	 * Sends the conversation to analysis-service and returns its result. Throws if it can't be reached,
	 * times out, or answers with an error status; the caller decides what a failure means.
	 */
	public AnalyzeResponse analyze(AnalyzeRequest request) {
		return restClient.post()
				.uri("/v1/analyze")
				.body(request) // Java record -> JSON
				.retrieve() // send it; throws on a 4xx/5xx answer
				.body(AnalyzeResponse.class); // JSON -> Java record
	}

}
