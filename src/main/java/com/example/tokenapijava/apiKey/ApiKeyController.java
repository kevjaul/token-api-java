package com.example.tokenapijava.apiKey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.tokenapijava.application.AppsSchema;
import com.example.tokenapijava.config.ApiKeyAuthenticationPrincipal;
import com.example.tokenapijava.scope.ApiKeyScopeId;
import com.example.tokenapijava.scope.ApiKeyScopeRepository;
import com.example.tokenapijava.scope.ApiKeyScopeSchema;
import com.example.tokenapijava.utils.HashUtil;

@Slf4j
@RestController
@RequestMapping("/api/apikeys")
@Tag(name = "API Keys", description="Gestion des clés API")
public class ApiKeyController {
    
    private ApiKeyRepository apiKeyRepository;

    private ApiKeyScopeRepository apiKeyScopeRepository;

    public ApiKeyController(ApiKeyRepository apiKeyRepository, ApiKeyScopeRepository apiKeyScopeRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyScopeRepository = apiKeyScopeRepository;
    }

    @PostMapping("/recycle")
    @Operation(summary = "Recycle votre clé d'API")
    @Tag(name = "API Keys")
    @SecurityRequirement(name = "apiKeyAuth")
    public ResponseEntity<?> recycleApiKey(Authentication auth) {
        ApiKeyAuthenticationPrincipal principal = (ApiKeyAuthenticationPrincipal) auth.getPrincipal();
        AppsSchema app = principal.requireApp();
        ApiKeySchema currentApiKey  = principal.getApiKey();
        if(currentApiKey.getRoleType() != Role.ADMIN){
            if(currentApiKey.getStatus() != Status.ACTIVE){
                if(currentApiKey.getStatus() == Status.ROTATING){
                    return ResponseEntity.badRequest().body("{\"error\": \"API_KEY_ROTATING\", \"message\": \"Your API Key is currently rotating. You can't recycle it right now.\"}");
                }
                return ResponseEntity.badRequest().body("{\"error\": \"API_KEY_ERROR\", \"message\": \"Your API Key is no longer active and usable. Please contact an administrator to recycle your API Key.\"}");
            } else if (currentApiKey.getCreatedAt().isAfter(Instant.now().minusSeconds(600))) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS.value()).body("{\"error\": \"TOO_MANY_REQUESTS\", \"message\": \"Please wait at least 10 minutes after key creation before recycling your API Key.\"}");
            }

            if(currentApiKey.getExpiresAt().isAfter(Instant.now().plusSeconds(60*60*24))){
                currentApiKey.setExpiresAt(Instant.now().plusSeconds(60*60*24)); // Old key expires in 24 hours
            }
            currentApiKey.setStatus(Status.ROTATING);
            apiKeyRepository.save(currentApiKey);
        }

        String newApiKeyValue = UUID.randomUUID().toString();
        Instant newExpiresAt = Instant.now().plusSeconds(60*60*24*30).truncatedTo(ChronoUnit.SECONDS);
        ApiKeySchema newApiKey = new ApiKeySchema(HashUtil.sha256(newApiKeyValue), Role.CLASSIC, Instant.now(), newExpiresAt, Status.ACTIVE);
        apiKeyRepository.save(newApiKey);
        log.info("Recycle key: From {}... to {}...", currentApiKey.getHashedApiKey().substring(0,12), newApiKey.getHashedApiKey().substring(0,12));
        ApiKeyScopeSchema newKeyScope = new ApiKeyScopeSchema(new ApiKeyScopeId(newApiKey.getHashedApiKey(), app.getId()));
        apiKeyScopeRepository.save(newKeyScope);
        log.info("Recycle key: New scope for {}... on app {}", newApiKey.getHashedApiKey().substring(0,12), app.getId());
        return ResponseEntity.ok().body("{\"new_api_key\": \"" + newApiKeyValue + "\", \"expiresAt\": \""+ newExpiresAt +"\"}");
    }
}
