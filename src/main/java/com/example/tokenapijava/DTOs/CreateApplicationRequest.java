package com.example.tokenapijava.DTOs;

import com.example.tokenapijava.Schemas.TokenRegenerationSchema;

import jakarta.validation.Valid;

public record CreateApplicationRequest(
    String appName,
     
    Long maxTokenAmount, 

    Long minTokenAmount,

    @Valid
    TokenRegenerationSchema tokenRegenerationTime
){}
