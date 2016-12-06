/*
Copyright 2016 Jacopo Rigoli

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package it.polimi.diceH2020.launcher.service;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * <strong>Wrap main RestTemplate functions to add @Retryable</strong> <br>
 * Useful for prototype components that can't support it.
 *
 * @author Jacopo Rigoli
 * @see RestTemplate
 * @see EnableRetry
 *
 */
@Service
public class RestCommunicationWrapper {

	private static final int maxRequests = 5;
	private static final int delayRequests = 5000; // [ms]
	private static final double multiplierRequests = 1.5;

	private RestTemplate restTemplate;

	private long startTime = System.currentTimeMillis();

	public RestCommunicationWrapper(){
		restTemplate = new RestTemplate();
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setBufferRequestBody(false);     //AVOID RUN OUT OF MEMORY
		restTemplate.setRequestFactory(requestFactory);
	}

	@Retryable(maxAttempts = maxRequests, backoff = @Backoff(delay = delayRequests,multiplier=multiplierRequests))
	public <T> T postForObject(String url, Object request, Class<T> responseType) throws Exception{
		//printTimeoutDuration();
		return restTemplate.postForObject(url, request, responseType);
	}

	@Retryable(maxAttempts = maxRequests, backoff = @Backoff(delay = delayRequests,multiplier=multiplierRequests))
	public <T> T getForObject(String url, Class<T> responseType) throws Exception{
		//printTimeoutDuration();
		return restTemplate.getForObject(url,responseType);
	}

	public void printTimeoutDuration(){
		long endTime = System.currentTimeMillis();
		long duration = (endTime - startTime);
		startTime = endTime;
		System.out.println("[DEBUG-Retry]"+duration);
	}
}
