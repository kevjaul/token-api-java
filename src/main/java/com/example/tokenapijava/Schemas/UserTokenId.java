package com.example.tokenapijava.Schemas;

import org.springframework.data.relational.core.mapping.Table;

@Table("USER_TOKENS")
public record UserTokenId(
    String userId,

    String linkedApp

)  {}
