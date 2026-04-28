package com.example.tokenapijava.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.tokenapijava.apiKey.ApiKeyRepository;
import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.apiKey.Role; 

import com.example.tokenapijava.utils.HashUtil;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private ApiKeyRepository apiKeyRepository;

    private final RateLimitService rateLimitService;
    
    public RateLimitFilter(ApiKeyRepository apiKeyRepository, RateLimitService rateLimitService) {
        this.apiKeyRepository = apiKeyRepository;
        this.rateLimitService = rateLimitService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String apiKeyValue = request.getHeader("X-Api-Key");
        if(apiKeyValue == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String hashedApiKey = HashUtil.sha256(apiKeyValue);
        ApiKeySchema apiKey = apiKeyRepository.findByHashedApiKey(hashedApiKey).orElseThrow();
        if(apiKey.getRoleType() == Role.ADMIN) {
            filterChain.doFilter(request, response);
            return;
        }
        String method = request.getMethod();

        Bucket bucket = rateLimitService.resolveBucket(hashedApiKey, method);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if(probe.isConsumed()){
            response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
            filterChain.doFilter(request, response);
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000L;

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(waitForRefill));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> body = Map.of("error", "Too many requests", "message", "Rate limit exceeded. Please wait for " + waitForRefill + " seconds before retrying.");
            objectMapper.writeValue(response.getWriter(), body);
        } 
    }
}
