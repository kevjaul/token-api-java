package com.example.tokenapijava.config;

import java.time.Instant;
import java.util.List;
import java.time.temporal.ChronoUnit;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;

import org.slf4j.MDC;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.tokenapijava.apiKey.ApiKeyRepository;
import com.example.tokenapijava.apiKey.ApiKeySchema;
import com.example.tokenapijava.apiKey.Role;
import com.example.tokenapijava.apiKey.Status;
import com.example.tokenapijava.application.AppsSchema;
import com.example.tokenapijava.application.SubscribedApplicationRepository;
import com.example.tokenapijava.scope.ApiKeyScopeRepository;
import com.example.tokenapijava.token.TokenRepository;
import com.example.tokenapijava.token.TokenService;


@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyCleanupJobs {
    
    private final ApiKeyRepository apiKeyRepository;

    private final ApiKeyScopeRepository apiKeyScopeRepository;

    private final Scheduler scheduler;

    private final SubscribedApplicationRepository appsRepository;

    private final TokenRepository tokenRepository;

    private final TokenService tokenService;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredApiKeysAndApps() throws SchedulerException {
        try{
            MDC.put("apiKey","[SYSTEM]");
            MDC.put("method","[SCHEDULER]");

            log.info("Core Scheduled Job: Running cleanup job.");
            Instant now = Instant.now();
            Instant deleteThreshold = now.minus(30, ChronoUnit.DAYS);

            // Delete expired and revoked API keys
            List<ApiKeySchema> keysToDelete = apiKeyRepository.findAllByStatusInAndExpiresAtBefore(
                List.of(Status.EXPIRED, Status.REVOKED), deleteThreshold
            );

            for (ApiKeySchema key : keysToDelete) {
                apiKeyScopeRepository.deleteById_HashedApiKey(key.getHashedApiKey());
                if(key.getRoleType() != Role.ADMIN){ // Do not delete admin keys
                    apiKeyRepository.delete(key);    
                    log.info("Core Scheduled Job: {} key deleted: {}...", key.getStatus(), key.getHashedApiKey().substring(0, 12));
                }
            }
            
            // Delete orphans applications
            List<AppsSchema> apps = appsRepository.findAll();

            for (AppsSchema app : apps) {
                boolean hasScopes =
                    apiKeyScopeRepository.existsById_appId(app.getId());

                if (!hasScopes) {
                    log.info("Core Scheduled Job: Application with appId={} doesn't have linked scope anymore. Deleting application...", app.getId());
                    tokenRepository.deleteAllById_AppId(app.getId());
                    log.info("Core Scheduled Job: All users deleted for application {} with appId={}", app.getAppName(), app.getId());
                    if(scheduler.checkExists(JobKey.jobKey("regen-" + app.getId()))){
                        tokenService.deleteAppSchedule(app.getId());
                    }
                    appsRepository.delete(app);
                    log.info("Core Scheduled Job: Application {} with appId={} deleted", app.getAppName(), app.getId());
                }
            }
            log.info("Core Scheduled Job: Finished cleanup job.");
        } finally {
            MDC.clear();
        }
        
    }

    @Scheduled(cron = "0 59 21 * * *")
    @Transactional
    public void checkAndUpdateExpiredApiKeys(){
        try{
            MDC.put("apiKey","[SYSTEM]");
            MDC.put("method","[SCHEDULER]");

            log.info("Core Scheduled Job: Running expired api keys check job.");    
            Instant now = Instant.now();

            List<ApiKeySchema> keysToExpire =
                apiKeyRepository.findAllByStatusInAndExpiresAtBefore(
                    List.of(Status.ACTIVE, Status.ROTATING), now
                );

            for (ApiKeySchema key : keysToExpire) {
                if (key.getRoleType() != Role.ADMIN) {
                    Status currentStatus = key.getStatus();
                    key.setStatus(Status.EXPIRED);
                    log.info("Core Scheduled Job: {}... key expired, set is status from {} to {}", key.getHashedApiKey().substring(0, 12), currentStatus, key.getStatus());
                }
            }
            log.info("Core Scheduled Job: Finished expired api keys check job.");
        } finally {
            MDC.clear();
        }
    }
}
