package com.example.tokenapijava;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.tokenapijava.Schemas.UserTokenId;
import com.example.tokenapijava.Schemas.UserTokenSchema;

public interface TokenRepository extends JpaRepository<UserTokenSchema, UserTokenId> {

}
