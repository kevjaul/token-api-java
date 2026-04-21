package com.example.tokenapijava.Conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.quartz.Job;
import org.quartz.JobExecutionContext;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.SubscribedApplicationRepository;

@Component
public class TokenRegenerationJob implements Job{
    
    @Autowired
    private TokenService tokenService;

    @Autowired
    private SubscribedApplicationRepository appsRepository;

    @Override
    public void execute(JobExecutionContext context) {
        String appId = context.getJobDetail()
            .getJobDataMap()
            .getString("appId");
        AppsSchema appToRegen = appsRepository.findByApiKey(appId).orElseThrow();
        tokenService.regenerateForApp(appToRegen,1L);
    }
}
