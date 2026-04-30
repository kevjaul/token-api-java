package com.example.tokenapijava.apiKey;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Entity
@Table(name = "API_KEYS")
@Access(AccessType.FIELD)
public class ApiKeySchema {

    @Id @NotBlank 
    @Column(name = "HASHED_API_KEY")
    private String hashedApiKey;   

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "ROLE_TYPE")
    private Role roleType;

    private Instant createdAt;

    private Instant expiresAt;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "KEY_STATUS")
    private Status status;
}
