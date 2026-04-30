package com.example.tokenapijava.scope;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Table(name = "API_KEYS_SCOPES")
@Access(AccessType.FIELD)
public class ApiKeyScopeSchema implements Serializable{
    @EmbeddedId
    private ApiKeyScopeId id;
}
