package com.example.tokenapijava.scope;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyScopeRepository extends JpaRepository<ApiKeyScopeSchema, ApiKeyScopeId> {
    public List<ApiKeyScopeSchema> findAllById_appId(Long appId);
    public Optional<ApiKeyScopeSchema> findById_HashedApiKey(String hashedApiKey);
    public void deleteAllById_appId(Long appId);
    public void deleteById_HashedApiKey(String hashedApiKey);
    public boolean existsById_appId(Long appId);
}
