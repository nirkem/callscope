package com.callscope;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The "callscope:" section of application.yml, as a typed object. Inject it where needed
 * instead of reading strings with @Value.
 */
@ConfigurationProperties("callscope")
public record CallscopeProperties(String queueName, String deadLetterQueueName, AnalysisService analysisService) {

	public record AnalysisService(URI baseUrl, Duration connectTimeout, Duration readTimeout) {
	}

}
