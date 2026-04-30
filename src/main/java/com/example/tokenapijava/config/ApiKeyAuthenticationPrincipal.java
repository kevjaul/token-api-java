package com.example.tokenapijava.config;

import lombok.*;

import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.application.AppsSchema;

@Getter
@Setter
@AllArgsConstructor
public class ApiKeyAuthenticationPrincipal{
    
    private final ApiKeySchema apiKey;

    private final AppsSchema app;

}