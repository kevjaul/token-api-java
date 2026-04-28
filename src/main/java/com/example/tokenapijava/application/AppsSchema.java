package com.example.tokenapijava.application;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "APPLICATIONS")
@Access(AccessType.FIELD)
public class AppsSchema{
     
    
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long Id;

    @NotBlank @JsonProperty("name") 
    private String appName;

    @JsonProperty("max_token_value") @Schema(defaultValue = "15")
    private Long maxTokenAmount;

    @JsonProperty("min_token_value") @Schema(defaultValue = "0") 
    private Long minTokenAmount;

    @Embedded @JsonProperty("token_regeneration_time") 
    private TokenRegenerationSchema tokenRegenerationTime;

}