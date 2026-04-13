package com.example.tokenapijava;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.example.tokenapijava.Schemas.AppsSchema;

import java.util.Optional;

public interface SubscribedApplicationRepository extends CrudRepository<AppsSchema, Long>, PagingAndSortingRepository<AppsSchema, Long>{
    AppsSchema findByAppName(String appName);
    Optional<AppsSchema> findByApiKey(String apiKey);
}
