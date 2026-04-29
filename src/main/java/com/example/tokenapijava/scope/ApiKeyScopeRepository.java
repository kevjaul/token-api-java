package com.example.tokenapijava.scope;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyScopeRepository extends JpaRepository<ApiKeyScopeSchema, ApiKeyScopeId> {
    public Optional<ApiKeyScopeSchema> findById_appId(Long appId);
    public ApiKeyScopeSchema findById_HashedApiKey(String hashedApiKey);
}
