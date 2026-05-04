package com.example.tokenapijava.apiKey;

import java.time.Instant;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.tokenapijava.utils.HashUtil;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminKeyInitializer implements CommandLineRunner{
    
    private final ApiKeyRepository apiKeyRepository;

    @Value("${admin.api.key}")
    private String adminKey;

    @Override
    public void run(String... args) { 
        
        if (adminKey == null || adminKey.isBlank()){
            throw new IllegalStateException("Environment variable 'admin.api.key' must be defined.");
        }

        String hashAdminKey = HashUtil.sha256(adminKey);

        boolean adminKeyAlreadyExists = apiKeyRepository.existsByHashedApiKeyAndRoleType(hashAdminKey, Role.ADMIN);

        if(!adminKeyAlreadyExists){
            ApiKeySchema adminKeySchema = new ApiKeySchema(hashAdminKey, Role.ADMIN, Instant.now(), null, Status.ACTIVE);
            apiKeyRepository.save(adminKeySchema);
            log.info("Admin key created.");
        } else {
            log.info("Admin key already exists. Skip task.");
        }
    }
}
