package com.example.tokenapijava.json;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.Schemas.TokenRegenerationSchema;


@JsonTest
class ApplicationsJsonTests {

    @Autowired
    private JacksonTester<AppsSchema> json;
    
    @Autowired
    private JacksonTester<AppsSchema[]> jsonList;

    private List<AppsSchema> applications;

    @BeforeEach
    void setUp(){
        TokenRegenerationSchema regenerationSchema = new TokenRegenerationSchema(1, 12, 0);
        TokenRegenerationSchema regenerationSchema2 = new TokenRegenerationSchema(1, 22, 0);
        applications = List.of(
            new AppsSchema(1L, "testApp", "xxa", 15L, 0L,regenerationSchema),
            new AppsSchema(2L, "testApp2", "xxb", 300L, 0L, regenerationSchema2)
        );
    }

    @Test
    void applicationSerializationTest() throws IOException {
        AppsSchema application = applications.get(0);
        assertThat(json.write(application)).isStrictlyEqualToJson("OneApplication.json");
        assertThat(json.write(application)).hasJsonPathNumberValue("@.Id");
        assertThat(json.write(application)).extractingJsonPathNumberValue("@.Id").isEqualTo(1);
        assertThat(json.write(application)).hasJsonPathNumberValue("@.token_regeneration_time.days");
        assertThat(json.write(application)).extractingJsonPathNumberValue("@.token_regeneration_time.days").isEqualTo(applications.get(0).tokenRegenerationTime().days());
    }

    @Test
    void applicationDeserializationTest() throws IOException {
        ClassPathResource source = new ClassPathResource("com/example/tokenapijava/json/OneApplication.json");
        String jsonObject = Files.readString(source.getFile().toPath());
        assertThat(json.parse(jsonObject)).isEqualTo(applications.get(0));
        assertThat(json.parseObject(jsonObject).Id()).isEqualTo(applications.get(0).Id());
        assertThat(json.parseObject(jsonObject).tokenRegenerationTime().days()).isEqualTo(applications.get(0).tokenRegenerationTime().days());
    }
}
