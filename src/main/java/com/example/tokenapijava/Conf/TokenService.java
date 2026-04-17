package com.example.tokenapijava.Conf;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;

import com.example.tokenapijava.Schemas.AppsSchema;
import com.example.tokenapijava.TokenRepository;

@Service
public class TokenService {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private TokenRepository tokenRepository;

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
            .withSchedule(schedule)
            .startNow()
            .build();

        scheduler.scheduleJob(job, trigger);
    }

}