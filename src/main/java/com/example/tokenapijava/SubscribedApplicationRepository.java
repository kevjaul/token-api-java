package com.example.tokenapijava;

import org.springframework.data.repository.CrudRepository;

import com.example.tokenapijava.Schemas.AppsSchema;

interface SubscribedApplicationRepository extends CrudRepository<AppsSchema, Long>{
    AppsSchema findByAppName(String appName);
}
