package com.example.tokenapijava;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.example.tokenapijava.DTOs.CreateApplicationRequest;
import com.example.tokenapijava.Schemas.TokenRegenerationSchema;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@AutoConfigureRestTestClient 
@ActiveProfiles("test")
public class ApplicationsTests {
    @Autowired
    TestRestTemplate restTemplate;

    @Test
    @DirtiesContext
    void shouldCreateANewApplication() {
        TokenRegenerationSchema tokenRegenerationTime = new TokenRegenerationSchema(1, 12, 0);
        CreateApplicationRequest application = new CreateApplicationRequest("testApp", 15L, 0L, tokenRegenerationTime);
        ResponseEntity<Void> createAppResponse = restTemplate
            .withBasicAuth("userTest1", "aaa111")
            .postForEntity("/api/apps/register", application, Void.class);
        assertThat(createAppResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
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
        assertThat(appsCount).isEqualTo(2);
        
        JSONArray appsNames = documentContext.read("$..name");
        assertThat(appsNames).containsExactlyInAnyOrder("testApp","testApp2");

        JSONArray maxTokenAmounts = documentContext.read("$..max_token_value");
        assertThat(maxTokenAmounts).containsExactlyInAnyOrder(15,300);
    }
}
