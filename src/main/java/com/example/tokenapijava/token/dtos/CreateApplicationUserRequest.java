package com.example.tokenapijava.token.dtos;

public record CreateApplicationUserRequest(
    String userId,

    Long initialTokens
){}