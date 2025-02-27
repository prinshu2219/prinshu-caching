package com.kdu.caching.service;


import com.kdu.caching.exception.GeocodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static com.kdu.caching.logger.Logs.log;

/**
 * GeocodingService handles the retrieval of coordinates (latitude and longitude)
 * using address, also the reverse lookup of address from coordinates.
 * It utilizes caching to optimize repeated requests.
 */
@Service
public class GeocodingService {

    @Value("${geocoding-url}")
    private String geocodingUrl;

    @Value("${reverse-geocoding-url}")
    private String reverseGeocodingUrl;

    @Value("${api-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final CacheManager cacheManager;

    public GeocodingService(RestTemplate restTemplate, CacheManager cacheManager){
        this.restTemplate = restTemplate;
        this.cacheManager = cacheManager;
    }

    // Method to get the Coordinates from API using address
    public Map<String, Double> getCoordinates(String address){
        //Get the cache values
        Cache cache = cacheManager.getCache("geocoding");

        // To check if the address is already present in cache and address is not goa
        if (cache!=null && !address.equalsIgnoreCase("goa")) {
            Map<String, Double> cachedCoordinates = cache.get(address, Map.class);
            if (cachedCoordinates != null) {
                log.info("Cache hit for address: " + address);
                return cachedCoordinates;
            }
        }

        // Format the Url with access key and address
        String url = String.format("%s?access_key=%s&query=%s", geocodingUrl, apiKey, address);
        ResponseEntity<Map> response=restTemplate.getForEntity(url,Map.class);
        if (response.getBody() == null) {
            throw new GeocodingException("No data received for address: " + address);
        }
        //Get the Json body with 'data'
        List<Map<String,Object>> data=(List<Map<String, Object>>) response.getBody().get("data");

        // Get the first Latitude and Longitude data
        if (data!=null && !data.isEmpty()){
            Map<String,Object> firstResult = data.get(0);
            Map<String,Double> coordinates = new HashMap<>();
            coordinates.put("latitude",((Number)firstResult.get("latitude")).doubleValue());
            coordinates.put("longitude",((Number)firstResult.get("longitude")).doubleValue());

            // If the address is goa, Don't add in cache
            if (cache != null && !address.equalsIgnoreCase("goa")) {
                cache.put(address, coordinates);
                log.info("Cached coordinates for address: " + address + "  " + coordinates);
            }
            return coordinates;
        }
        throw new GeocodingException("No data received for address: ");
    }

    // Method to get the Address from API using coordinates
    public String getAddress(double latitude, double longitude){
        Cache cache = cacheManager.getCache("reverse-geocoding");
        String cacheKey = latitude + "," + longitude;

        // To check if the address is already present in cache
        if (cache != null) {
            String cachedAddress=cache.get(cacheKey, String.class);
            if (cachedAddress!=null){
                log.info("Cache hit for coordinates: " + latitude + ", " + longitude);
                return cachedAddress;
            }
        }

        // Format the Url with access key and coordinates
        String url = String.format("%s?access_key=%s&query=%f,%f", reverseGeocodingUrl, apiKey, latitude, longitude);
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        if (response.getBody() == null) {
            throw new GeocodingException("No data received for coordinates: " + latitude + ", " + longitude);
        }
        //Get the Json body with 'data'
        List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");

        // Get the first Address data
        if (data != null && !data.isEmpty()) {
            String address = (String) data.get(0).get("label");

            if (cache != null) {
                cache.put(cacheKey, address);
                log.info("Cached address for coordinates: [" + latitude + ", " + longitude + "] -> " + address);
            }
            return address;
        }

        throw new GeocodingException("Invalid coordinates or no data found");
    }

    // Method to evict/delete cache
    public void evictCache(String key) {
        Cache geocodingCache = cacheManager.getCache("geocoding");
        Cache reverseGeocodingCache = cacheManager.getCache("reverse-geocoding");

        if (key == null) {
            if (geocodingCache != null) geocodingCache.clear();
            if (reverseGeocodingCache != null) reverseGeocodingCache.clear();
            log.info("Evicted all cache entries");
        } else {
            if (geocodingCache != null) geocodingCache.evict(key);
            if (reverseGeocodingCache != null) reverseGeocodingCache.evict(key);
            log.info("Evicted cache entry for key: " + key);
        }
    }
}
