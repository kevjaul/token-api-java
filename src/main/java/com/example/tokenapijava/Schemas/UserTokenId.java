package com.example.tokenapijava.Schemas;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Table;

import java.io.Serializable;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
@Table(name = "USER_TOKENS")
@Access(AccessType.FIELD)
public class UserTokenId implements Serializable{
    String userId;

    String linkedApp;

}
