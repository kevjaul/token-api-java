package com.example.tokenapijava.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.Schemas.UserTokenId;
import com.example.tokenapijava.Schemas.UserTokenSchema;

@JsonTest
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
        assertThat(jsonNode.has("Id")).isTrue();
        assertThat(jsonNode.get("Id").has("userId")).isTrue();
        assertThat(jsonNode.get("Id").get("userId").asText()).isEqualTo("userTest1");
        assertThat(jsonNode.get("Id").has("linkedApp")).isTrue();
        assertThat(jsonNode.get("Id").get("linkedApp").asText()).isEqualTo("xxa");
    }

    @Test
    void userTokenDeserializationTest() throws IOException {
        ClassPathResource source = new ClassPathResource("com/example/tokenapijava/json/OneUserToken.json");
        String jsonObject = Files.readString(source.getFile().toPath());
        assertThat(json.parse(jsonObject)).isEqualTo(usersTokens.get(0));
        assertThat(json.parseObject(jsonObject).Id()).isEqualTo(usersTokens.get(0).Id());
        assertThat(json.parseObject(jsonObject).tokenAmount()).isEqualTo(usersTokens.get(0).tokenAmount());
    }
}
