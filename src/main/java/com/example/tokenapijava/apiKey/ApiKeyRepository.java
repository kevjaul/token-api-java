package com.example.tokenapijava.apiKey;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKeySchema, String>{
    public boolean existsByHashedApiKeyAndRoleType(String hashedApiKey, Role roleType);
    public void deleteAllByHashedApiKey(String hashedApiKey);
    public Optional<ApiKeySchema> findByHashedApiKey(String hashedApiKey);
    public List<ApiKeySchema> findAllByStatusInAndExpiresAtBefore(List<Status> statuses, Instant expiresAt);
}
