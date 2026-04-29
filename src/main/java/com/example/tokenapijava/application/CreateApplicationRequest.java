package com.example.tokenapijava.application;

import jakarta.validation.Valid;

public record CreateApplicationRequest(
    String appName,
     
    Long maxTokenAmount, 

    Long minTokenAmount,

    @Valid
    TokenRegenerationSchema tokenRegenerationTime
){}
