package com.example.tokenapijava.apiKey;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKeySchema, String>{
    public boolean existsByHashedApiKeyAndRoleType(String hashedApiKey, Role roleType);
    public void deleteAllByHashedApiKey(String hashedApiKey);
    public Optional<ApiKeySchema> findByHashedApiKey(String hashedApiKey);
}
