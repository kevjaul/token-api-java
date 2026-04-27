package com.example.tokenapijava;

import jakarta.transaction.Transactional;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.tokenapijava.Conf.TokenService;
import com.example.tokenapijava.DTOs.CreateApplicationUserRequest;
import com.example.tokenapijava.DTOs.ManageTokensRequest;
import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.Schemas.UserTokenId;
import com.example.tokenapijava.Schemas.UserTokenSchema;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tokens")
@SecurityRequirement(name = "apiKeyAuth")
@Tag(name = "Tokens", description = "Gestion des tokens")
public class TokenController {

    private TokenRepository tokenRepository;

    private TokenService tokenService;

    public TokenController(TokenRepository tokenRepository, TokenService tokenService) {
        this.tokenRepository = tokenRepository;
        this.tokenService = tokenService;
    }
    
    @PostMapping("/register")
    @Operation(summary = "Enregistre un nouvel utilisateur avec un nombre de tokens initial.")
    @Tag(name = "Tokens")
    public ResponseEntity<?> createAnApplicationUser(@RequestBody CreateApplicationUserRequest applicationUser, UriComponentsBuilder Ucb,Authentication auth ) {
        AppsSchema app = (AppsSchema) auth.getPrincipal();
        UserTokenId userId = new UserTokenId(applicationUser.userId(),app.getHashedApiKey());
        if(tokenRepository.existsById(userId)){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        } 
        else if(applicationUser.initialTokens() < app.getMinTokenAmount() || applicationUser.initialTokens() > app.getMaxTokenAmount()){
            return ResponseEntity.badRequest().body("Initial tokens must be between " + app.getMinTokenAmount() + " and " + app.getMaxTokenAmount());
        }
        UserTokenSchema newAppUser = new UserTokenSchema(userId,applicationUser.initialTokens());
        UserTokenSchema savedAppUser = tokenRepository.save(newAppUser);
        URI locationOfNewUser = Ucb
            .path("/api/tokens/{id}")
            .buildAndExpand(savedAppUser.getId().getUserId())
            .toUri();
        return ResponseEntity.created(locationOfNewUser).build();
    }
    
    @GetMapping("/{userId}")
    @Operation(summary = "Récupère le nombre de tokens d'un utilisateur.")
    @Tag(name = "Tokens")
    public ResponseEntity<?> getUserTokensAmount(@PathVariable String userId, Authentication auth){
        AppsSchema app = (AppsSchema) auth.getPrincipal();
        UserTokenId id = new UserTokenId(userId,app.getHashedApiKey());
        UserTokenSchema userToken = tokenRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(userToken.getTokenAmount());
    }

    @PostMapping("/{userId}/add")
    @Operation(summary = "Ajoute des tokens à un utilisateur.")
    @Tag(name = "Tokens")
    public ResponseEntity<?> addUserTokens(@PathVariable String userId, @RequestBody ManageTokensRequest manageTokens, Authentication auth){
        AppsSchema app = (AppsSchema) auth.getPrincipal();
        UserTokenId id = new UserTokenId(userId,app.getHashedApiKey());
        UserTokenSchema userToken = tokenRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (manageTokens.amount() <= 0){
            return ResponseEntity.badRequest().body("You should add at least 1 token");
        }
        else if (userToken.getTokenAmount() == app.getMaxTokenAmount()){
            return ResponseEntity.ok().build();
        }
        else if( manageTokens.amount() > app.getMaxTokenAmount() - userToken.getTokenAmount()){
            userToken.setTokenAmount(app.getMaxTokenAmount());
            tokenRepository.save(userToken);
            return ResponseEntity.ok().body("{\"message\": \"Reach max token amount for this user\", \"currentTokenAmount\": " + userToken.getTokenAmount() + "}");
        } 
        userToken.setTokenAmount(userToken.getTokenAmount() + manageTokens.amount());
        tokenRepository.save(userToken);
        return ResponseEntity.ok().body("{\"message\": \"Tokens added\", \"currentTokenAmount\": " + userToken.getTokenAmount() + "}");
    }
    
    @PostMapping("/{userId}/subtract")
    @Operation(summary = "Enlève des tokens à un utilisateur.")
    @Tag(name = "Tokens")
    public ResponseEntity<?> subtractUserTokens(@PathVariable String userId, @RequestBody ManageTokensRequest manageTokens, Authentication auth) {
        AppsSchema app = (AppsSchema) auth.getPrincipal();
        UserTokenId id = new UserTokenId(userId,app.getHashedApiKey());
        UserTokenSchema userToken = tokenRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (manageTokens.amount() <= 0){
            return ResponseEntity.badRequest().body("You should delete at least 1 token");
        }
        else if (userToken.getTokenAmount() == app.getMinTokenAmount()){
            return ResponseEntity.ok().build();
        }
        else if( manageTokens.amount() > userToken.getTokenAmount() - app.getMinTokenAmount()){
            userToken.setTokenAmount(app.getMinTokenAmount());
            tokenRepository.save(userToken);
            return ResponseEntity.ok().body("{\"message\": \"Reach min token amount for this user\", \"currentTokenAmount\": " + userToken.getTokenAmount() + "}");
        } 
        userToken.setTokenAmount(userToken.getTokenAmount() - manageTokens.amount());
        tokenRepository.save(userToken);
        return ResponseEntity.ok().body("{\"message\": \"Tokens subtracted\", \"currentTokenAmount\": " + userToken.getTokenAmount() + "}");
    }

    @PostMapping("/regenerate")
    @Operation(summary = "Rajoute des tokens à tous les utilisateurs d'une application.")
    @Tag(name = "Tokens")
    public ResponseEntity<?> regenerateTokensForAllUsers(@RequestBody ManageTokensRequest manageTokens, Authentication auth) {
        AppsSchema app = (AppsSchema) auth.getPrincipal();
        if (manageTokens.amount() <= 0){
            return ResponseEntity.badRequest().body("You should add at least 1 token");
        }
        tokenService.regenerateForApp(app,manageTokens.amount());
        return ResponseEntity.ok().body("{\"message\": \"Tokens regenerated manually\"}");
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Supprime un utilisateur.")
    @Tag(name = "Tokens")
    public ResponseEntity<?> deleteUserTokens(@PathVariable String userId, Authentication auth) {
        AppsSchema app = (AppsSchema) auth.getPrincipal();
        UserTokenSchema userToken = tokenRepository.findById_LinkedAppAndId_UserId(app.getHashedApiKey(), userId);
        if(userToken != null){
            tokenRepository.delete(userToken);
        }
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @DeleteMapping("/")
    @Operation(summary = "Supprime tous les utilisateurs d'une application.")
    @Tag(name = "Tokens")
    public ResponseEntity<?> deleteAllUsersTokens(Authentication auth) {
        AppsSchema app = (AppsSchema) auth.getPrincipal();
        tokenRepository.deleteAllById_LinkedApp(app.getHashedApiKey());
        return ResponseEntity.noContent().build();
    }
    
    
}
