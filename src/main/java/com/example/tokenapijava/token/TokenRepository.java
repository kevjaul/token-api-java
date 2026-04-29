package com.example.tokenapijava.token;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRepository extends JpaRepository<UserTokenSchema, UserTokenId> {
    public List<UserTokenSchema> findAllById_AppId(Long appId);
    public Optional<UserTokenSchema> findById_AppIdAndId_UserId(Long appId, String userId);
    public void deleteAllById_AppId(Long appId);
}
