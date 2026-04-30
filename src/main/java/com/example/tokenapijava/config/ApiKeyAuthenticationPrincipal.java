package com.example.tokenapijava.config;

import lombok.*;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.application.AppsSchema;

@Getter
@Setter
@AllArgsConstructor
public class ApiKeyAuthenticationPrincipal{
    
    private final ApiKeySchema apiKey;

    private final AppsSchema app;

    public AppsSchema requireApp() {
        if (this.app == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "X-Target-App header is required"
            );
        }
        return this.app;
    }
}