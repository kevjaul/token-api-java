package com.example.tokenapijava.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.Schemas.UserTokenId;
import com.example.tokenapijava.Schemas.UserTokenSchema;

@JsonTest
@ActiveProfiles("test")
class TokensJsonTests {
    @Autowired
    private JacksonTester<UserTokenSchema> json;
    
    @Autowired
    private JacksonTester<AppsSchema[]> jsonList;

    private List<UserTokenSchema> usersTokens;

    @BeforeEach
    void setUp(){
        UserTokenId userTokenId = new UserTokenId("userTest1", "xxa");
        UserTokenSchema userToken1 = new UserTokenSchema(userTokenId, 3L);
        usersTokens = List.of(userToken1);
    }

    @Test
    void userTokenSerializationTest() throws IOException {
        UserTokenSchema userToken = usersTokens.get(0);
        assertThat(json.write(userToken)).isStrictlyEqualToJson("OneUserToken.json");
        assertThat(json.write(userToken)).hasJsonPathNumberValue("@.tokenAmount");
        assertThat(json.write(userToken)).extractingJsonPathNumberValue("@.tokenAmount").isEqualTo(3);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(json.write(userToken).getJson());
        assertThat(jsonNode.has("id")).isTrue();
        assertThat(jsonNode.get("id").has("userId")).isTrue();
        assertThat(jsonNode.get("id").get("userId").asText()).isEqualTo("userTest1");
        assertThat(jsonNode.get("id").has("linkedApp")).isTrue();
        assertThat(jsonNode.get("id").get("linkedApp").asText()).isEqualTo("xxa");
    }

    @Test
    void userTokenDeserializationTest() throws IOException {
        ClassPathResource source = new ClassPathResource("com/example/tokenapijava/json/OneUserToken.json");
        String jsonObject = Files.readString(source.getFile().toPath());
        
        UserTokenSchema actual = json.parseObject(jsonObject);

        UserTokenSchema expected = usersTokens.get(0);

        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
        assertThat(actual.getId()).usingRecursiveComparison().isEqualTo(expected.getId());
        assertThat(actual.getTokenAmount()).isEqualTo(expected.getTokenAmount());
    }
}
