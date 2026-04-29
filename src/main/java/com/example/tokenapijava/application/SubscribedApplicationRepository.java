package com.example.tokenapijava.application;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SubscribedApplicationRepository extends JpaRepository<AppsSchema, Long>, PagingAndSortingRepository<AppsSchema, Long>{
    AppsSchema findByAppName(String appName);
}
