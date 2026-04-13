package com.example.tokenapijava;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.tokenapijava.DTOs.CreateApplicationUserRequest;

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
    private String createAnApplicationUser(@RequestBody CreateApplicationUserRequest applicationUser, UriComponentsBuilder Ucb) {
        //TODO: process POST request
        return "toto";
    }
    
}
