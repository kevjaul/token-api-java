package com.example.tokenapijava;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import com.example.tokenapijava.config.RateLimitService;
import com.example.tokenapijava.token.dtos.CreateApplicationUserRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureRestTestClient 
@ActiveProfiles("test")
public class ConfTests {

    @Autowired
    RateLimitService rateLimitService;

    @Autowired
    TestRestTemplate restTemplate;

    @Value("${admin.api.key}")
    private String adminKey;

    @BeforeEach
    void setUp() {
        rateLimitService.clearAll();
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql",
        "data/usersTokensTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes     
    void shouldTriggerRateLimitForGET() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        HttpEntity<Void> request = new HttpEntity<>(headers);
        ResponseEntity<?> response;
        for(int i = 0; i < 20; i++){
            response = restTemplate.exchange("/api/tokens/userTest1", HttpMethod.GET, request, Long.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getHeaders().get("X-RateLimit-Remaining")).isNotNull();
            assertThat(Integer.parseInt(response.getHeaders().get("X-RateLimit-Remaining").get(0))).isEqualTo(19- i);
            assertThat(response.getHeaders().get("Retry-After")).isNull();
        }
        response = restTemplate.exchange("/api/tokens/userTest1", HttpMethod.GET, request, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(response.getHeaders().get("X-RateLimit-Remaining")).isNotNull();
        assertThat(Integer.parseInt(response.getHeaders().get("X-RateLimit-Remaining").get(0))).isEqualTo(0);
        assertThat(response.getHeaders().get("Retry-After")).isNotNull();
    }

    @Test
    @Sql(scripts = {"data/clean.sql",
        "data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD) //Register a valid API key for testing purposes     
    void shouldTriggerRateLimitForDELETE() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", "xxa");
        ResponseEntity<?> response;
        for(int i = 0; i < 6; i++){
            CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest"+i, 3L);
            HttpEntity<?> request = new HttpEntity<>(applicationUser, headers);
            response = restTemplate.exchange("/api/tokens/register",HttpMethod.POST, request, Void.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

            request = new HttpEntity<>(headers);
            response = restTemplate.exchange("/api/tokens/userTest"+i,HttpMethod.DELETE, request, String.class);
            if(i < 5){
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            } else {
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            }
        }
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD) // Required to fully setup default admin key on app restart
    @Sql(scripts = {"data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldNotTriggerRateLimitIfAnADMINApiKeyIsUsed() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", adminKey);
        headers.set("X-Target-App", "1");
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);
        ResponseEntity<Void> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        assertThat(createUserResponse.getHeaders().get("X-RateLimit-Remaining")).isNull();        
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD) // Required to fully setup default admin key on app restart
    @Sql(scripts = {"data/applicationsTestDatas.sql"}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    void shouldReturnUnauthorizedIfAnADMINApiKeyIsUsedWithoutXTargetAppHeader() {
        CreateApplicationUserRequest applicationUser = new CreateApplicationUserRequest("userTest1", 3L);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", adminKey);
        HttpEntity<CreateApplicationUserRequest> request = new HttpEntity<>(applicationUser, headers);
        ResponseEntity<Void> createUserResponse = restTemplate
            .postForEntity("/api/tokens/register", request, Void.class);
        assertThat(createUserResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
