package it.polimi.diceH2020.launcher.service;

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
		//ObjectMapper om = new ObjectMapper().registerModule(new Jdk8Module());
		restTemplate = new RestTemplate();
		
//		MappingJackson2HttpMessageConverter jsonMessageConverter = new MappingJackson2HttpMessageConverter(om);
//		List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
//		messageConverters.add(jsonMessageConverter);
//		restTemplate.setMessageConverters(messageConverters);
		
//		MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter(om);
//		//messageConverter.setObjectMapper(om);
//		restTemplateForJson.getMessageConverters().removeIf(m -> m.getClass().getName().equals(MappingJackson2HttpMessageConverter.class.getName()));
//		restTemplateForJson.getMessageConverters().add(messageConverter);
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
