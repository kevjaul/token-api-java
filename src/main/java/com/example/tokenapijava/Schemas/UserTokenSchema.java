package com.example.tokenapijava.Schemas;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import io.swagger.v3.oas.annotations.media.Schema;

import com.example.tokenapijava.Schemas.UserTokenId;

@Table("TOKENS")
public record UserTokenSchema( 
    @Id
    UserTokenId Id, 
    
    @Schema(defaultValue = "0")
    Long tokenAmount 
    
) {}
