package com.example.tokenapijava.scope;

import jakarta.annotation.Nullable;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Table(name = "SCOPES")
@Access(AccessType.FIELD)
public class ApiKeyScopeId implements Serializable{
    @Column(name = "HASHED_API_KEY")
    private String hashedApiKey;

    @Nullable
    @Column(name = "APP_ID")
    private Long appId;
}
