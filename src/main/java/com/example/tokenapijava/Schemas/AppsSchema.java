package com.example.tokenapijava.Schemas;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import io.swagger.v3.oas.annotations.media.Schema;

@Table("APPLICATIONS")
public record AppsSchema(
    @Id 
    Long Id, 

    @NotBlank @JsonProperty("name") 
    String appName, 

    @NotBlank 
    String apiKey,

    @JsonProperty("max_token_value") @Schema(defaultValue = "15")
    Long maxTokenAmount, 

    @JsonProperty("min_token_value") @Schema(defaultValue = "0") 
    Long minTokenAmount, 

    @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL) @JsonProperty("token_regeneration_time") 
    TokenRegenerationSchema tokenRegenerationTime
) {}