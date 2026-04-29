package com.example.tokenapijava;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import java.net.URI;

import net.minidev.json.JSONArray;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import com.example.tokenapijava.apiKey.ApiKeyRepository;
import com.example.tokenapijava.application.CreateApplicationRequest;
import com.example.tokenapijava.application.SubscribedApplicationRepository;
import com.example.tokenapijava.application.TokenRegenerationSchema;
import com.example.tokenapijava.config.RateLimitService;
import com.example.tokenapijava.scope.ApiKeyScopeRepository;
import com.example.tokenapijava.token.TokenRepository;
import com.example.tokenapijava.token.TokenService;
import com.example.tokenapijava.token.UserTokenId;
import com.example.tokenapijava.token.UserTokenSchema;
import com.example.tokenapijava.utils.HashUtil;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureRestTestClient 
@ActiveProfiles("test")
public class ApplicationsTests {

    @Autowired
    ApiKeyRepository apiKeyRepository;

    @Autowired
    ApiKeyScopeRepository apiKeyScopeRepository;

    @Autowired
    RateLimitService rateLimitService;

    @Autowired
    Scheduler scheduler;

    @Autowired
    SubscribedApplicationRepository applicationRepository;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    TokenService tokenService;

    @BeforeEach
    void setUp() {
        rateLimitService.clearAll();
    }
    
    @Test
    @Sql(scripts = {"data/clean.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)    
    void shouldCreateANewApplication() {
        TokenRegenerationSchema tokenRegenerationTime = new TokenRegenerationSchema(1, 12, 0);
        CreateApplicationRequest application = new CreateApplicationRequest("testApp", 15L, 0L, tokenRegenerationTime);
        ResponseEntity<String> createAppResponse = restTemplate
            .withBasicAuth("userTest1", "aaa111")
            .postForEntity("/api/apps/register", application, String.class);
        assertThat(createAppResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        URI location = createAppResponse.getHeaders().getLocation();
        assertThat(location).isNotNull();
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
                "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)    
    void shouldReturnAllApplications() {
        ResponseEntity<String> allAppsResponse = restTemplate
            .withBasicAuth("userTest1", "aaa111")
            .getForEntity("/api/apps/list", String.class);
        assertThat(allAppsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        DocumentContext documentContext = JsonPath.parse(allAppsResponse.getBody());
        int appsCount = documentContext.read("$.length()");
        assertThat(appsCount).isEqualTo(3);
        
        JSONArray appsNames = documentContext.read("$..name");
        assertThat(appsNames).containsExactlyInAnyOrder("testApp","testApp2","testAppRegen");

        JSONArray maxTokenAmounts = documentContext.read("$..max_token_value");
        assertThat(maxTokenAmounts).containsExactlyInAnyOrder(15,300,300);
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes 
    void shouldDeleteAnApplicationAndAllReferencees() throws SchedulerException{
        String hashedApiKey = HashUtil.sha256("apiKeyForRegenTests");
        Long appId = 3L;
        UserTokenSchema userToken = new UserTokenSchema(new UserTokenId("tempUser",appId), 0L);
        tokenRepository.save(userToken);
        tokenService.scheduleAppJob(appId, 30L);
        // Check that the job has successfully been scheduled 
        assertThat(scheduler.checkExists(JobKey.jobKey("regen-" + appId ))).isTrue();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "apiKeyForRegenTests");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<Void> deleteApplicationResponse = restTemplate
            .exchange("/api/apps/myApp", HttpMethod.DELETE, request, Void.class);
        assertThat(deleteApplicationResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // No more DB entries
        assertThat(tokenRepository.findAllById_AppId(appId)).isEmpty();
        assertThat(applicationRepository.findById(appId)).isEmpty();
        assertThat(apiKeyRepository.findByHashedApiKey(hashedApiKey)).isEmpty();
        assertThat(apiKeyScopeRepository.findById_HashedApiKey(hashedApiKey)).isNull();
        // No more job in scheduler
        assertThat(scheduler.checkExists(JobKey.jobKey("regen-" + appId))).isFalse();
    }
}
