package com.example.tokenapijava;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import com.example.tokenapijava.Schemas.AppsSchema;

interface SubscribedApplicationRepository extends CrudRepository<AppsSchema, Long>, PagingAndSortingRepository<AppsSchema, Long>{
    AppsSchema findByAppName(String appName);
}
