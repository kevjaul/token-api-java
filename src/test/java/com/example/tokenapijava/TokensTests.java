package com.example.tokenapijava;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tokenapijava.DTOs.CreateApplicationUserRequest;
import com.example.tokenapijava.Schemas.UserTokenId;
import com.example.tokenapijava.Schemas.UserTokenSchema;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureRestTestClient 
public class TokensTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Test
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    @Sql(scripts="data/applicationsTestDatas.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes
    void shouldCreateANewTokenUser() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);
        ResponseEntity<Void> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    @DirtiesContext
    void shouldNotCreateANewTokenUserIfNotUsingApiKey() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
        ResponseEntity<Void> createUserResponse = restTemplate
            .withBasicAuth("userTest1", "aaa111")
            .postForEntity("/api/tokens/register", applicationUser, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DirtiesContext
    void shouldNotCreateANewTokenUserIfApiKeyNotExist() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
         HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "notexistingAPIKey");
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);

        ResponseEntity<Void> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

}
