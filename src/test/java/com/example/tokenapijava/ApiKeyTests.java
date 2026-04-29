package com.example.tokenapijava;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.example.tokenapijava.apiKey.ApiKeyRepository;
import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.apiKey.Role;
import com.example.tokenapijava.application.CreateApplicationRequest;
import com.example.tokenapijava.application.TokenRegenerationSchema;
import com.example.tokenapijava.scope.ApiKeyScopeRepository;
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
        assertThat(apiKeyScopeRepository.findById_appId(createdAppId)).isPresent();
    }
}
