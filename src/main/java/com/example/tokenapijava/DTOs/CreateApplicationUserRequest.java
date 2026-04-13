package com.example.tokenapijava.DTOs;

public record CreateApplicationUserRequest(
    String userId,

    Long initialTokens
){}
