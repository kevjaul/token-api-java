package com.example.tokenapijava.config;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.application.AppsSchema;

@Slf4j
@Getter
@Setter
@AllArgsConstructor
public class ApiKeyAuthenticationPrincipal{
    
    private final ApiKeySchema apiKey;

    private final AppsSchema app;

    public AppsSchema requireApp() {
        if (this.app == null) {
            log.error("Mandatory headers: ADMIN Key used without mandatory X-Target-App header.");
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "X-Target-App header is required"
            );
        }
        return this.app;
    }
}