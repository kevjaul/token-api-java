package com.example.tokenapijava.json;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.apiKey.Role;
import com.example.tokenapijava.apiKey.Status;
import com.example.tokenapijava.utils.HashUtil;

@JsonTest
@ActiveProfiles("test")
class ApiKeysJsonTests {

    @Autowired
    private JacksonTester<ApiKeySchema> json;
    
    @Autowired
    private JacksonTester<ApiKeySchema[]> jsonList;

    private List<ApiKeySchema> apiKeys;

    @BeforeEach
    void setUp(){
        apiKeys = List.of(
            new ApiKeySchema(HashUtil.sha256("xxa"), Role.CLASSIC, Instant.parse("2026-04-27T00:00:00Z"),Instant.parse("2026-04-28T00:00:00Z"), Status.ACTIVE),
            new ApiKeySchema(HashUtil.sha256("xxb"), Role.CLASSIC, Instant.parse("2026-04-27T00:00:00Z"),Instant.parse("2026-04-28T00:00:00Z"), Status.ACTIVE)
        );
    }

    @Test
    void applicationSerializationTest() throws IOException {
        ApiKeySchema apiKey = apiKeys.get(0);
        assertThat(json.write(apiKey)).isStrictlyEqualToJson("OneApiKey.json");
        assertThat(json.write(apiKey)).hasJsonPathStringValue("@.hashedApiKey");
        assertThat(json.write(apiKey)).extractingJsonPathStringValue("@.hashedApiKey").isEqualTo(HashUtil.sha256("xxa"));
        assertThat(json.write(apiKey)).hasJsonPathStringValue("@.roleType");
        assertThat(json.write(apiKey)).extractingJsonPathStringValue("@.roleType").isEqualTo("CLASSIC");
        assertThat(json.write(apiKey)).hasJsonPathStringValue("@.createdAt");
        assertThat(json.write(apiKey)).extractingJsonPathStringValue("@.createdAt").isEqualTo("2026-04-27T00:00:00Z");
        assertThat(json.write(apiKey)).hasJsonPathStringValue("@.expiresAt");
        assertThat(json.write(apiKey)).extractingJsonPathStringValue("@.expiresAt").isEqualTo("2026-04-28T00:00:00Z");
        assertThat(json.write(apiKey)).hasJsonPathStringValue("@.status");
        assertThat(json.write(apiKey)).extractingJsonPathStringValue("@.status").isEqualTo("ACTIVE");
    }

    @Test
    void applicationDeserializationTest() throws IOException {
        ClassPathResource source = new ClassPathResource("com/example/tokenapijava/json/OneApiKey.json");
        String jsonObject = Files.readString(source.getFile().toPath());

        ApiKeySchema actual = json.parseObject(jsonObject);

        ApiKeySchema expected = apiKeys.get(0);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
