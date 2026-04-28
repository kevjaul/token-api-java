package com.example.tokenapijava.token;

import java.util.concurrent.TimeUnit;
import java.util.List;

import org.quartz.SchedulerException;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.tokenapijava.application.AppsSchema;
import com.example.tokenapijava.application.SubscribedApplicationRepository;

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
            tokenService.scheduleAppJob(app.getId(), 
                app.getTokenRegenerationTime().getDays() * 24 * 60
                + app.getTokenRegenerationTime().getHours() * 60
                + app.getTokenRegenerationTime().getMins(), TimeUnit.MINUTES);
        }
    }
}