package com.example.tokenapijava;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;

import org.quartz.SchedulerException;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import com.example.tokenapijava.Conf.TokenService;
import com.example.tokenapijava.DTOs.CreateApplicationUserRequest;
import com.example.tokenapijava.DTOs.ManageTokensRequest;
import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.Schemas.UserTokenId;
import com.example.tokenapijava.Schemas.UserTokenSchema;
import com.example.tokenapijava.SubscribedApplicationRepository;
import com.example.tokenapijava.TokenRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureRestTestClient 
@ActiveProfiles("test")
public class TokensTests {

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    SubscribedApplicationRepository applicationRepository;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    TokenService tokenService;
    
    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes     
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
    void shouldNotCreateANewTokenUserIfNotUsingApiKey() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
        ResponseEntity<Void> createUserResponse = restTemplate
            .withBasicAuth("userTest1", "aaa111")
            .postForEntity("/api/tokens/register", applicationUser, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldNotCreateANewTokenUserIfApiKeyNotExist() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
         HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "notexistingAPIKey");
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);

        ResponseEntity<Void> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @Sql(scripts = {
        "data/clean.sql",
         "data/applicationsTestDatas.sql", //Register a valid API key for testing purposes
         "data/usersTokensTestDatas.sql"},
          executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) 
    void shouldNotCreateANewTokenUserIfUserAlreadyExistsForApplication() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);
        ResponseEntity<Void> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldNotCreateANewTokenUserIfTokenAmountExceededForApplication() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3000L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);
        ResponseEntity<Void> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldGetTokenAmountForUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Long> tokenAmountResponse = restTemplate
            .exchange("/api/tokens/userTest1", HttpMethod.GET, request, Long.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tokenAmountResponse.getBody()).isEqualTo(3L);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldNotGetTokenAmountForNonExistingUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Void> tokenAmountResponse = restTemplate
            .exchange("/api/tokens/userTest2", HttpMethod.GET, request, Void.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldAddTokenAmountForUser() {
        ManageTokensRequest manageToken = new ManageTokensRequest(7L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<String> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/userTest1/add", request, String.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(tokenAmountResponse.getBody());
        Long tokenAmount = documentContext.read("$.currentTokenAmount", Long.class);
        assertThat(tokenAmount).isEqualTo(10L);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldNotAddTokenAmountForNonExistingUser() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Void> tokenAmountResponse = restTemplate
            .exchange("/api/tokens/userTest2", HttpMethod.GET, request, Void.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldSetMaxTokenAmountForUserIfTokenAmountExceeded() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        AppsSchema currentApp = applicationRepository.findByApiKey("xxa").orElseThrow();
        ManageTokensRequest manageToken = new ManageTokensRequest(currentApp.getMaxTokenAmount() - 1L);
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<String> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/userTest1/add", request, String.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(tokenAmountResponse.getBody());
        Long tokenAmount = documentContext.read("$.currentTokenAmount", Long.class);
        assertThat(tokenAmount).isEqualTo(currentApp.getMaxTokenAmount());
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldNotAddIfTokenAmountIsNegativeOrZero() {
        ManageTokensRequest manageToken = new ManageTokensRequest(-7L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<Void> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/userTest1/add", request, Void.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ManageTokensRequest manageToken2 = new ManageTokensRequest(0L);
        HttpEntity<ManageTokensRequest> request2 = new HttpEntity<>(manageToken2, headers);
        ResponseEntity<Void> tokenAmountResponse2 = restTemplate
            .postForEntity("/api/tokens/userTest1/add", request2, Void.class);
        assertThat(tokenAmountResponse2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldSubtractTokenAmountForUser() {
        ManageTokensRequest manageToken = new ManageTokensRequest(2L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<String> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/userTest1/subtract", request, String.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(tokenAmountResponse.getBody());
        Long tokenAmount = documentContext.read("$.currentTokenAmount", Long.class);
        assertThat(tokenAmount).isEqualTo(1L);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldNotSubtractTokenAmountForNonExistingUser() {
        ManageTokensRequest manageToken = new ManageTokensRequest(1L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<Void> tokenAmountResponse = restTemplate
            .exchange("/api/tokens/userTest2/subtract", HttpMethod.POST, request, Void.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldSetMinTokenAmountForUserIfTokenAmountExceeded() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        AppsSchema currentApp = applicationRepository.findByApiKey("xxa").orElseThrow();
        ManageTokensRequest manageToken = new ManageTokensRequest(currentApp.getMaxTokenAmount() + 1L);
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<String> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/userTest1/subtract", request, String.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        DocumentContext documentContext = JsonPath.parse(tokenAmountResponse.getBody());
        Long tokenAmount = documentContext.read("$.currentTokenAmount", Long.class);
        assertThat(tokenAmount).isEqualTo(currentApp.getMinTokenAmount());
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldNotSubtractIfTokenAmountIsNegativeOrZero() {
        ManageTokensRequest manageToken = new ManageTokensRequest(-7L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<Void> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/userTest1/subtract", request, Void.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ManageTokensRequest manageToken2 = new ManageTokensRequest(0L);
        HttpEntity<ManageTokensRequest> request2 = new HttpEntity<>(manageToken2, headers);
        ResponseEntity<Void> tokenAmountResponse2 = restTemplate
            .postForEntity("/api/tokens/userTest1/subtract", request2, Void.class);
        assertThat(tokenAmountResponse2.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldRegenerateTokenForAllUserOfApplication() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        ManageTokensRequest manageToken = new ManageTokensRequest(1L);
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<String> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/regenerate", request, String.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserTokenSchema userTest1 = tokenRepository.findById(new UserTokenId("userTest1", "xxa")).orElseThrow();
        UserTokenSchema userTest3 = tokenRepository.findById(new UserTokenId("userTest3", "xxa")).orElseThrow();
        assertThat(userTest1.getTokenAmount()).isEqualTo(3+1);
        assertThat(userTest3.getTokenAmount()).isEqualTo(13+1);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldNotRegenerateTokenIfMaxTokenAmountAlreadyReached() {
        AppsSchema currentApp = applicationRepository.findByApiKey("xxb").orElseThrow();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxb");
        ManageTokensRequest manageToken = new ManageTokensRequest(1L);
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<String> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/regenerate", request, String.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserTokenSchema userTest5 = tokenRepository.findById(new UserTokenId("userTest5", "xxb")).orElseThrow();
        assertThat(userTest5.getTokenAmount()).isEqualTo(currentApp.getMaxTokenAmount());
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldRegenerateToMaxTokenAmountIfNewAmountExceedMaxTokenAmountForApplication() {
        AppsSchema currentApp = applicationRepository.findByApiKey("xxb").orElseThrow();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxb");
        ManageTokensRequest manageToken = new ManageTokensRequest(45L);
        HttpEntity<ManageTokensRequest> request = new HttpEntity<>(manageToken, headers);
        ResponseEntity<String> tokenAmountResponse = restTemplate
            .postForEntity("/api/tokens/regenerate", request, String.class);
        assertThat(tokenAmountResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserTokenSchema userTest5 = tokenRepository.findById(new UserTokenId("userTest5", "xxb")).orElseThrow();
        assertThat(userTest5.getTokenAmount()).isEqualTo(currentApp.getMaxTokenAmount());
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldRegenerateTokensFromRealScheduling() throws SchedulerException, InterruptedException {
        String appApiKey = "apkiKeyForRegenTests";
        UserTokenSchema userToken = new UserTokenSchema(new UserTokenId("tempUser", appApiKey), 0L);
        tokenRepository.save(userToken);

        tokenService.scheduleAppJob(appApiKey, 3, TimeUnit.SECONDS);

        Thread.sleep(10000);

        UserTokenSchema updatedUser = tokenRepository.findById_LinkedAppAndId_UserId(appApiKey, "tempUser");
        assertThat(updatedUser.getTokenAmount()).isGreaterThan(0L);

    }
    
}
