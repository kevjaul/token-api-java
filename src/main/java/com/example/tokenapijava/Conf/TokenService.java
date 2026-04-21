package com.example.tokenapijava.Conf;

import java.util.concurrent.TimeUnit;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import org.springframework.stereotype.Service;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.TokenRepository;

@Service
public class TokenService {

    private Scheduler scheduler;

    private TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository, Scheduler scheduler) {
        this.tokenRepository = tokenRepository;
        this.scheduler = scheduler;
    }

    public void regenerateForApp(AppsSchema app, Long tokenToAdd) {
        Long appMaxTokenAmount = app.getMaxTokenAmount();
        tokenRepository.findAllById_LinkedApp(app.getApiKey()).forEach(userToken -> {
            long currentUserTokenAmount = userToken.getTokenAmount();
            if (currentUserTokenAmount == appMaxTokenAmount){
                return;
            }
            else if( tokenToAdd > appMaxTokenAmount - currentUserTokenAmount){
                userToken.setTokenAmount(appMaxTokenAmount);
            }
            else{
                userToken.setTokenAmount(currentUserTokenAmount + tokenToAdd);
            }
            tokenRepository.save(userToken);
        });
    }

    // Default value is minutes
    public void scheduleAppJob(String appId, long interval) throws SchedulerException {
        scheduleAppJob(appId, interval, TimeUnit.MINUTES);
    }

    public void scheduleAppJob(String appId, long interval, TimeUnit unit) throws SchedulerException {
        JobDetail job = JobBuilder.newJob(TokenRegenerationJob.class)
            .withIdentity("regen-" + appId)
            .usingJobData("appId", appId)
            .storeDurably()
            .build();

        SimpleScheduleBuilder schedule = null;
        switch(unit){
            case SECONDS:
                schedule = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds((int) interval);
                break;
            case MINUTES:
                schedule = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInMinutes((int) interval);
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
        schedule = schedule.repeatForever();
        
        Trigger trigger = TriggerBuilder.newTrigger()
            .withIdentity("regen-trigger-" + appId)
            .forJob("regen-" + appId)
            .withSchedule(schedule)
            .startNow()
            .build();

        if(scheduler.checkExists(JobKey.jobKey("regen-" + appId))){
            scheduler.rescheduleJob(TriggerKey.triggerKey("regen-trigger-" + appId), trigger);
        } else {
            scheduler.scheduleJob(job, trigger);    
        }
        System.out.println("Job created for " + appId + " with job key regen-" + appId);
    }

    public void deleteAppSchedule(String appId) throws SchedulerException {
        JobKey jobKey = JobKey.jobKey("regen-" + appId);
        scheduler.deleteJob(jobKey);
        System.out.println("Job deleted for " + appId + " with job key regen-" + appId);   
    }
}