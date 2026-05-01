package com.example.tokenapijava.token;

import java.util.concurrent.TimeUnit;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

import org.quartz.SchedulerException;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.example.tokenapijava.application.AppsSchema;
import com.example.tokenapijava.application.SubscribedApplicationRepository;


@Slf4j
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
        log.info("Core Scheduled Job: Syncing jobs with database.");
        List<AppsSchema> allApps = appsRepository.findAll();
        for(AppsSchema app : allApps){
            tokenService.scheduleAppJob(app.getId(), 
                app.getTokenRegenerationTime().getDays() * 24 * 60
                + app.getTokenRegenerationTime().getHours() * 60
                + app.getTokenRegenerationTime().getMins(), TimeUnit.MINUTES);
            log.info("Core Scheduled Job: Starting job scheduled for application with appId={}", app.getId());
        }
        log.info("Core Scheduled Job: Finished syncing jobs with database.");
    }
}