package com.kdu.caching.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class with RestTemplate and CacheManager
 * for HTTP Requests and Caching respectively
 */
@Configuration
public class AppConfig {

    //Bean for RestTemplate
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
    //Bean for CacheManager
    @Bean
    public CacheManager cacheManager(){
        return new ConcurrentMapCacheManager("geocoding","reverse-geocoding");
    }
}
