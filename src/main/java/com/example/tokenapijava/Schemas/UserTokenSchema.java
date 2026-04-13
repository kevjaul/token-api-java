package com.example.tokenapijava.Schemas;

import org.springframework.data.annotation.Id;

import io.swagger.v3.oas.annotations.media.Schema;
public record UserTokenSchema(
    @Id 
    Long Id, 
    
    @Schema(defaultValue = "0")
    Integer tokenAmount, 
    
    String linkedApp) {

}
