package com.example.tokenapijava.Conf;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class HashUtil {

    public static String sha256(String apiKey){
        try{
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(apiKey.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);     
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }   
    }
}
