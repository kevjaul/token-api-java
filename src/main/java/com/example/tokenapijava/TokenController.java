package com.example.tokenapijava;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.tokenapijava.DTOs.CreateApplicationUserRequest;
import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.Schemas.UserTokenId;
import com.example.tokenapijava.Schemas.UserTokenSchema;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
 
@RestController
@RequestMapping("/api/tokens")
@SecurityRequirement(name = "apiKeyAuth")
class TokenController {

    private final TokenRepository tokenRepository;

    private TokenController(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }
    
    @PostMapping("/register")
    @Operation(summary = "Enregistre un nouvel utilisateur avec un nombre de tokens initial.")
    @Tag(name = "Tokens")
    private ResponseEntity<?> createAnApplicationUser(@RequestBody CreateApplicationUserRequest applicationUser, UriComponentsBuilder Ucb,Authentication auth ) {
        AppsSchema app = (AppsSchema) auth.getPrincipal();
        UserTokenId userId = new UserTokenId(applicationUser.userId(),app.apiKey());
        if(tokenRepository.existsById(userId)){
            return ResponseEntity.badRequest().body("User already exists");
        } 
        else if(applicationUser.initialTokens() < app.minTokenAmount() || applicationUser.initialTokens() > app.maxTokenAmount()){
            return ResponseEntity.badRequest().body("Initial tokens must be between " + app.minTokenAmount() + " and " + app.maxTokenAmount());
        }
        UserTokenSchema newAppUser = new UserTokenSchema(userId,applicationUser.initialTokens());
        UserTokenSchema savedAppUser = tokenRepository.save(newAppUser);
        URI locationOfNewUser = Ucb
            .path("/api/tokens/{id}")
            .buildAndExpand(savedAppUser.Id())
            .toUri();
        return ResponseEntity.created(locationOfNewUser).build();
    }
    
}
