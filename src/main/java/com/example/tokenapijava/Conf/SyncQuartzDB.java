package com.example.tokenapijava.Conf;

import org.quartz.SchedulerException;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.SubscribedApplicationRepository;

@Component
public class SyncQuartzDB {
    private final SubscribedApplicationRepository appsRepository;
    
    private final TokenService tokenService;

    public SyncQuartzDB(SubscribedApplicationRepository appsRepository, TokenService tokenService) {
        this.appsRepository = appsRepository;
        this.tokenService = tokenService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncAllJobs() throws SchedulerException{
        List<AppsSchema> allApps = appsRepository.findAll();
        for(AppsSchema app : allApps){
            tokenService.scheduleAppJob(app.getApiKey(), 
                app.getTokenRegenerationTime().getDays() * 24 * 60
                + app.getTokenRegenerationTime().getHours() * 60
                + app.getTokenRegenerationTime().getMins(), TimeUnit.MINUTES);
        }
    }
}