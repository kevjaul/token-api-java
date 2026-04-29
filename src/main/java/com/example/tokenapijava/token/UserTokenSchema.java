package com.example.tokenapijava.token;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

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
    private UserTokenId id;
    
    @Schema(defaultValue = "0")
    @Column(name = "TOKEN_AMOUNT")
    private Long tokenAmount;
    
}
