package com.example.tokenapijava.token;

import lombok.extern.slf4j.Slf4j;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.tokenapijava.application.AppsSchema;
import com.example.tokenapijava.application.SubscribedApplicationRepository;

@Slf4j
@Component
public class TokenRegenerationJob implements Job{
    
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SubscribedApplicationRepository appsRepository;

    @Override
    public void execute(JobExecutionContext context) {
        Long appId = context.getJobDetail()
            .getJobDataMap()
            .getLong("appId");
        AppsSchema appToRegen = appsRepository.findById(appId).orElseThrow();
        tokenService.regenerateForApp(appToRegen,1L);
        log.info("Core Scheduled Job: Regenerated tokens for application with appId={}.", appId);
    }
}
