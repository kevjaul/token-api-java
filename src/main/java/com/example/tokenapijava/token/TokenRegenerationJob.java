package com.example.tokenapijava.token;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.tokenapijava.application.AppsSchema;
import com.example.tokenapijava.application.SubscribedApplicationRepository;

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
    }
}
