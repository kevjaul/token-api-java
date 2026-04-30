package com.example.tokenapijava;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.net.URI;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.example.tokenapijava.apiKey.ApiKeyRepository;
import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.apiKey.Role;
import com.example.tokenapijava.apiKey.Status;
import com.example.tokenapijava.application.CreateApplicationRequest;
import com.example.tokenapijava.application.TokenRegenerationSchema;
import com.example.tokenapijava.scope.ApiKeyScopeId;
import com.example.tokenapijava.scope.ApiKeyScopeRepository;
import com.example.tokenapijava.scope.ApiKeyScopeSchema;
import com.example.tokenapijava.token.dtos.CreateApplicationUserRequest;
import com.example.tokenapijava.utils.HashUtil;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureRestTestClient 
@ActiveProfiles("test")
public class ApiKeyTests {

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    ApiKeyScopeRepository apiKeyScopeRepository;

    @Autowired
    TestRestTemplate restTemplate;

    @Value("${admin.api.key}")
    private String adminKeyValue;

    @Test
    @Sql(scripts = {"data/clean.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes     
    void shouldCreateCLIENTApiKeyOnApplicationRegistration(){
        TokenRegenerationSchema tokenRegenerationTime = new TokenRegenerationSchema(1, 12, 0);
        CreateApplicationRequest application = new CreateApplicationRequest("testApp", 15L, 0L, tokenRegenerationTime);
        ResponseEntity<String> createAppResponse = restTemplate
            .withBasicAuth("userTest1", "aaa111")
            .postForEntity("/api/apps/register", application, String.class);
        assertThat(createAppResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = createAppResponse.getHeaders().getLocation();

        DocumentContext documentContext = JsonPath.parse(createAppResponse.getBody());
        String createdApiKey = documentContext.read("$.api_key", String.class);
        assertThat(apiKeyRepository.findByHashedApiKey(HashUtil.sha256(createdApiKey))).isPresent();

        ApiKeySchema apiKey = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(createdApiKey)).get();
        assertThat(apiKey.getRoleType()).isEqualTo(Role.CLASSIC);

        String path = location.getPath();
        Long createdAppId = Long.parseLong(path.substring(path.lastIndexOf("/") + 1));
        assertThat(apiKeyScopeRepository.findAllById_appId(createdAppId)).isNotEmpty();
        assertThat(apiKeyScopeRepository.findAllById_appId(createdAppId)).contains(new ApiKeyScopeSchema(new ApiKeyScopeId(HashUtil.sha256(createdApiKey),createdAppId)));
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldUnauthorizedIfApiKeyIsExpired() throws InterruptedException{
        String apiKey = "xxa";
        ApiKeySchema newApiKey = new ApiKeySchema(HashUtil.sha256(apiKey), Role.CLASSIC, Instant.now(), Instant.now().plusSeconds(3), Status.ACTIVE);
        apiKeyRepository.save(newApiKey);

        Thread.sleep(4000);
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);
        ResponseEntity<String> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, String.class);

        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        DocumentContext documentContext = JsonPath.parse(createUserResponse.getBody());
        String errorKey = documentContext.read("$.error");
        assertThat(errorKey).isEqualTo("API_KEY_EXPIRED");
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldUnauthorizedIfApiKeyIsRevoked(){
        String apiKey = "xxa";
        ApiKeySchema newApiKey = new ApiKeySchema(HashUtil.sha256(apiKey), Role.CLASSIC, Instant.now(), Instant.now().plusSeconds(60*60*24*30), Status.REVOKED);
        apiKeyRepository.save(newApiKey);

        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", apiKey);
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);
        ResponseEntity<String> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, String.class);

        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        DocumentContext documentContext = JsonPath.parse(createUserResponse.getBody());
        String errorKey = documentContext.read("$.error");
        assertThat(errorKey).isEqualTo("API_KEY_REVOKED");
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldRecycleACLIENTApiKeyWithAGracePeriod(){
        String currentApiKeyValue = "xxa";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", currentApiKeyValue);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> recycleResponse = restTemplate
            .postForEntity("/api/apikeys/recycle", request, String.class);

        assertThat(recycleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiKeyRepository.findByHashedApiKey(HashUtil.sha256(currentApiKeyValue))).isPresent();
        ApiKeySchema currentApiKey = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(currentApiKeyValue)).get();
        assertThat(currentApiKey.getStatus()).isEqualTo(Status.ROTATING);
        assertThat(currentApiKey.getExpiresAt()).isBefore(Instant.now().plusSeconds(60*60*24));
        
        DocumentContext documentContext = JsonPath.parse(recycleResponse.getBody());
        String newApiKeyValue = documentContext.read("$.new_api_key");
        assertThat(apiKeyRepository.findByHashedApiKey(HashUtil.sha256(newApiKeyValue))).isPresent();
        ApiKeySchema newApiKey = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(newApiKeyValue)).get();
        assertThat(newApiKey.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(newApiKey.getRoleType()).isEqualTo(Role.CLASSIC);
        String newExpiresAt = documentContext.read("$.expiresAt");
        assertThat(newApiKey.getExpiresAt()).isEqualTo(Instant.parse(newExpiresAt));
        assertThat(newApiKey.getExpiresAt()).isAfter(Instant.now().plusSeconds(60*60*24 - 5)); // 5 seconds tolerance for account for processing and execution time variability
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldNotRecycleACLIENTApiKeyIfAlreadyRotating(){
        String currentApiKeyValue = "xxa";
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", currentApiKeyValue);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> recycleResponse = restTemplate
            .postForEntity("/api/apikeys/recycle", request, String.class);

        assertThat(recycleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiKeyRepository.findByHashedApiKey(HashUtil.sha256(currentApiKeyValue)).get().getStatus()).isEqualTo(Status.ROTATING);
        Instant beforeRotationExpiresAt = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(currentApiKeyValue)).get().getExpiresAt(); 
        ResponseEntity<String> retryRecycleResponse = restTemplate
            .postForEntity("/api/apikeys/recycle", request, String.class);
        assertThat(retryRecycleResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        DocumentContext documentContext = JsonPath.parse(retryRecycleResponse.getBody());
        String errorKey = documentContext.read("$.error");
        assertThat(errorKey).isEqualTo("API_KEY_ROTATING");
        assertThat(apiKeyRepository.findByHashedApiKey(HashUtil.sha256(currentApiKeyValue)).get().getExpiresAt()).isEqualTo(beforeRotationExpiresAt);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldNotRecycleACLIENTApiKeyBeforeCooldownEnded(){
        String currentApiKeyValue = "xxa";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", currentApiKeyValue);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<String> recycleResponse = restTemplate
            .postForEntity("/api/apikeys/recycle", request, String.class);

        assertThat(recycleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        DocumentContext documentContext = JsonPath.parse(recycleResponse.getBody());
        String newApiKeyValue = documentContext.read("$.new_api_key");
        ApiKeySchema newApiKey = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(newApiKeyValue)).get();
        assertThat(newApiKey.getStatus()).isEqualTo(Status.ACTIVE);

        headers = new HttpHeaders();
        headers.set("X-Api-Key", newApiKeyValue);
        HttpEntity<Void> newRequest = new HttpEntity<>(headers);
        ResponseEntity<String> retryRecycleResponse = restTemplate
            .postForEntity("/api/apikeys/recycle", newRequest, String.class);
        assertThat(retryRecycleResponse.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(apiKeyRepository.findByHashedApiKey(HashUtil.sha256(newApiKeyValue)).get().getStatus()).isEqualTo(Status.ACTIVE);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD) // Required to fully setup default admin key on app restart
    @Sql(scripts = {"data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldNotModifyCurrentApiKeyIfItsAnADMINKey(){
        ApiKeySchema adminKey = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(adminKeyValue)).get();
        assertThat(adminKey.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(adminKey.getRoleType()).isEqualTo(Role.ADMIN);
        Instant adminExpiresAt = adminKey.getExpiresAt();
        assertThat(adminExpiresAt).isNull();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", adminKeyValue);
        headers.set("X-Target-App", "1");
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(headers);
        ResponseEntity<String> recycleResponse = restTemplate
            .postForEntity("/api/apikeys/recycle", request, String.class);
        
        assertThat(recycleResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiKeySchema adminKeyAfterRequest = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(adminKeyValue)).get();
        assertThat(adminKeyAfterRequest.getStatus()).isEqualTo(Status.ACTIVE);
        assertThat(adminKeyAfterRequest.getRoleType()).isEqualTo(Role.ADMIN);
        assertThat(adminKeyAfterRequest.getExpiresAt()).isEqualTo(adminExpiresAt);

        DocumentContext documentContext = JsonPath.parse(recycleResponse.getBody());
        String newApiKeyValue = documentContext.read("$.new_api_key");
        assertThat(apiKeyRepository.findByHashedApiKey(HashUtil.sha256(newApiKeyValue))).isPresent();
        assertThat(apiKeyScopeRepository.findById_HashedApiKey(HashUtil.sha256(newApiKeyValue))).isPresent();
        assertThat(apiKeyScopeRepository.findById_HashedApiKey(HashUtil.sha256(newApiKeyValue)).get().getId().getAppId()).isEqualTo(1L);
    }
}
