package com.example.tokenapijava.Conf;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.DAYS)
        .maximumSize(100_000)
        .build();

    public Bucket resolveBucket(String key, String method){
        String cacheKey = key + method;
        return cache.get(cacheKey, k -> createBucket(method));
    }

    private Bucket createBucket(String method){
        
        long capacity;
        Duration refillDuration = Duration.ofDays(1);

        switch (method) {
            case "DELETE":
                capacity = 5;
                break;
            case "GET":
            case "POST":
            case "PUT":
                capacity = 20;
                break;
            default:
                capacity = 10;
        }

        Bandwidth limit = Bandwidth.builder()
            .capacity(capacity)
            .refillIntervally(capacity, refillDuration)
            .build();
        
        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    public void clearAll(){
        cache.invalidateAll();
    }
}
