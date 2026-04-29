package com.example.tokenapijava.application;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.tokenapijava.apiKey.ApiKeyRepository;
import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.apiKey.Role;
import com.example.tokenapijava.scope.ApiKeyScopeId;
import com.example.tokenapijava.scope.ApiKeyScopeRepository;
import com.example.tokenapijava.scope.ApiKeyScopeSchema;
import com.example.tokenapijava.token.TokenRepository;
import com.example.tokenapijava.token.TokenService;
import com.example.tokenapijava.utils.HashUtil;

@RestController
@RequestMapping("/api/apps")
@Tag(name = "Applications", description="Gestion des applications")
public class SubscribedApplicationController {

    private ApiKeyRepository apiKeyRepository;

    private ApiKeyScopeRepository apiKeyScopeRepository;

    private Scheduler scheduler;

    private final SubscribedApplicationRepository appsRepository;

    private TokenRepository tokenRepository;

    private final TokenService tokenService;

    public SubscribedApplicationController(ApiKeyRepository apiKeyRepository, ApiKeyScopeRepository apiKeyScopeRepository, Scheduler scheduler,SubscribedApplicationRepository appsRepository, TokenRepository tokenRepository, TokenService tokenService ) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyScopeRepository = apiKeyScopeRepository;
        this.scheduler = scheduler;
        this.appsRepository = appsRepository;
        this.tokenRepository = tokenRepository;
        this.tokenService = tokenService;        
    }

    @PostMapping("/register")
    @Operation(summary = "Enregistre une nouvelle application et génère une clé API")
    @Tag(name = "Applications")
    public ResponseEntity<?> registerANewApplication(@Valid @RequestBody CreateApplicationRequest application, UriComponentsBuilder Ucb) throws SchedulerException {
        if (application.appName() == null || application.appName().isBlank() || application.appName().equalsIgnoreCase("string") ) {
            return ResponseEntity.badRequest().build();
        }
        AppsSchema appToCheck = appsRepository.findByAppName(application.appName());
        if (appToCheck != null) {
            //AppName already in use
            return ResponseEntity.notFound().build();
        }
        String apiKey = UUID.randomUUID().toString();
        AppsSchema newApp = new AppsSchema(null, application.appName(), application.maxTokenAmount(), application.minTokenAmount(), application.tokenRegenerationTime());
        AppsSchema savedApp = appsRepository.save(newApp);

        ApiKeySchema newApiKey = new ApiKeySchema(HashUtil.sha256(apiKey), Role.CLASSIC, Instant.now(), Instant.now().plusSeconds(60*60*24*30), false);
        ApiKeyScopeSchema keyScope = new ApiKeyScopeSchema(new ApiKeyScopeId(newApiKey.getHashedApiKey(), savedApp.getId()));
        apiKeyRepository.save(newApiKey);
        apiKeyScopeRepository.save(keyScope);

        URI locationOfNewApp = Ucb
            .path("/api/apps/{id}")
            .buildAndExpand(savedApp.getId())
            .toUri();
        
        long intervalMinutes = savedApp.getTokenRegenerationTime().getDays() * 24 * 60
            + savedApp.getTokenRegenerationTime().getHours() * 60
            + savedApp.getTokenRegenerationTime().getMins();
        tokenService.scheduleAppJob(savedApp.getId(), intervalMinutes);
        return ResponseEntity.created(locationOfNewApp).body("{\"api_key\": \"" + apiKey + "\"}");
    }

    @GetMapping("/list")
    @Operation(summary = "Liste toutes les applications enregistrées.")
    @Tag(name = "Applications")
    public ResponseEntity<List<AppsSchema>> listAllApplications(@PageableDefault(sort = "appName", direction = Sort.Direction.ASC) @ParameterObject Pageable pageable) {
        Page<AppsSchema> allApps = appsRepository.findAll(pageable);

        if (allApps.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(allApps.getContent());
    }
    
    @Transactional
    @DeleteMapping("/myApp")
    @Operation(summary = "Supprime l'application utilisant la clé API.")
    @Tag(name = "Applications")
    @SecurityRequirement(name = "apiKeyAuth")
    public ResponseEntity<?> deleteAnApplications(Authentication auth) throws SchedulerException{
        AppsSchema currentLoggedApp = (AppsSchema) auth.getPrincipal();
        tokenRepository.deleteAllById_AppId(currentLoggedApp.getId());
        if(scheduler.checkExists(JobKey.jobKey("regen-" + currentLoggedApp.getId()))){
            tokenService.deleteAppSchedule(currentLoggedApp.getId());
        }
        ApiKeyScopeSchema keyScope = apiKeyScopeRepository.findById_appId(currentLoggedApp.getId()).orElseThrow();
        apiKeyScopeRepository.delete(keyScope);
        apiKeyRepository.deleteAllByHashedApiKey(keyScope.getId().getHashedApiKey());
        appsRepository.delete(currentLoggedApp);
        return ResponseEntity.noContent().build();
    }
}