package com.example.tokenapijava;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TokenAPIMain {
    public static void main(String[] args) {
        SpringApplication.run(TokenAPIMain.class, args);
    }
}
