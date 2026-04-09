package com.example.tokenapijava;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.DTOs.CreateApplicationRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/apps")
class SubscribedApplicationController {

    private final SubscribedApplicationRepository appsRepository;

    public SubscribedApplicationController(SubscribedApplicationRepository appsRepository) {
        this.appsRepository = appsRepository;
    }

    @PostMapping("/register")
    @Operation(summary = "Enregistre une nouvelle application et génère une clé API")
    @Tag(name = "Applications")
    public ResponseEntity<?> registerANewApplication(@Valid @RequestBody CreateApplicationRequest application, UriComponentsBuilder Ucb) {
        if (application.appName() == null || application.appName().isBlank() || application.appName().equalsIgnoreCase("string") ) {
            return ResponseEntity.badRequest().build();
        }
        AppsSchema appToCheck = appsRepository.findByAppName(application.appName());
        if (appToCheck != null) {
            //AppName already in use
            return ResponseEntity.notFound().build();
        }
        AppsSchema newApp = new AppsSchema(null, application.appName(), UUID.randomUUID().toString(), application.maxTokenAmount(), application.minTokenAmount(), application.tokenRegenerationTime());
        AppsSchema savedApp = appsRepository.save(newApp);
        URI locationOfNewApp = Ucb
            .path("/api/apps/{id}")
            .buildAndExpand(savedApp.Id())
            .toUri();
        return ResponseEntity.created(locationOfNewApp).build();
    }
    
}
