package com.example.tokenapijava;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.example.tokenapijava.Schemas.UserTokenId;
import com.example.tokenapijava.Schemas.UserTokenSchema;

public interface TokenRepository extends JpaRepository<UserTokenSchema, UserTokenId> {
    public List<UserTokenSchema> findAllById_LinkedApp(String apiKey);
    public UserTokenSchema findById_LinkedAppAndId_UserId(String apiKey, String userId);
    public void deleteAllById_LinkedApp(String apiKey);
}
