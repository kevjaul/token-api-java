package com.example.tokenapijava;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.UUID;
import java.util.List;

import org.springdoc.core.annotations.ParameterObject;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.Conf.HashUtil;
import com.example.tokenapijava.Conf.TokenService;
import com.example.tokenapijava.DTOs.CreateApplicationRequest;

@RestController
@RequestMapping("/api/apps")
@Tag(name = "Applications", description="Gestion des applications")
public class SubscribedApplicationController {

    private final TokenService tokenService;

    private TokenRepository tokenRepository;

    private final SubscribedApplicationRepository appsRepository;

    private Scheduler scheduler;

    public SubscribedApplicationController(SubscribedApplicationRepository appsRepository, TokenService tokenService, TokenRepository tokenRepository,Scheduler scheduler) {
        this.appsRepository = appsRepository;
        this.tokenService = tokenService;
        this.tokenRepository = tokenRepository;
        this.scheduler = scheduler;
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
        AppsSchema newApp = new AppsSchema(null, application.appName(), HashUtil.sha256(apiKey), application.maxTokenAmount(), application.minTokenAmount(), application.tokenRegenerationTime());
        AppsSchema savedApp = appsRepository.save(newApp);
        URI locationOfNewApp = Ucb
            .path("/api/apps/{id}")
            .buildAndExpand(savedApp.getId())
            .toUri();
        long intervalMinutes = savedApp.getTokenRegenerationTime().getDays() * 24 * 60
            + savedApp.getTokenRegenerationTime().getHours() * 60
            + savedApp.getTokenRegenerationTime().getMins();
        tokenService.scheduleAppJob(savedApp.getHashedApiKey(), intervalMinutes);
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
        tokenRepository.deleteAllById_LinkedApp(currentLoggedApp.getHashedApiKey());
        if(scheduler.checkExists(JobKey.jobKey("regen-" + currentLoggedApp.getHashedApiKey()))){
            tokenService.deleteAppSchedule(currentLoggedApp.getHashedApiKey());
        }
        appsRepository.delete(currentLoggedApp);
        return ResponseEntity.noContent().build();
    }
}
