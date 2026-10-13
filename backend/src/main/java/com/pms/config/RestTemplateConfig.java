package com.pms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Registers RestTemplate as a Spring bean.
 * RestTemplate is Spring's synchronous HTTP client used by AiServiceClient.
 *
 * Note: For new projects, consider WebClient (reactive, non-blocking).
 * RestTemplate is still widely used and simpler for synchronous calls.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
