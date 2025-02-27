package com.kdu.caching.controller;

import com.kdu.caching.service.GeocodingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Rest Controller to retrieve address and coordinates using cache
 */
@RestController
@RequestMapping("/")
class GeocodingController {

    //Autowired with Service
    @Autowired
    private final GeocodingService geocodingService;

    public GeocodingController(GeocodingService geocodingService) {
        this.geocodingService = geocodingService;
    }

    // To get latitude and longitude from Address
    @GetMapping("geocoding")
    public ResponseEntity<Map<String, Double>> forwardGeocoding(@RequestParam String address) {
        return ResponseEntity.ok(geocodingService.getCoordinates(address));
    }

    // To get Address from Latitude and Longitude
    @GetMapping("reverse-geocoding")
    public ResponseEntity<String> reverseGeocoding(@RequestParam double latitude, @RequestParam double longitude) {
        return ResponseEntity.ok(geocodingService.getAddress(latitude, longitude));
    }

    // Delete from Cache
    @DeleteMapping("cache")
    public ResponseEntity<String> evictCache(@RequestParam(required = false) String key) {
        geocodingService.evictCache(key);
        return ResponseEntity.ok("Cache evicted");
    }
}
