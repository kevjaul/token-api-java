package com.example.tokenapijava.Schemas;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "TOKENS")
@Access(AccessType.FIELD)
public class UserTokenSchema{
    @EmbeddedId
    private UserTokenId Id;
    
    @Schema(defaultValue = "0")
    private Long tokenAmount;
    
}
