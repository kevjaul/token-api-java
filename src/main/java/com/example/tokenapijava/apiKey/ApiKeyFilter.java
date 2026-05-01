package com.example.tokenapijava.apiKey;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.example.tokenapijava.application.AppsSchema;
import com.example.tokenapijava.application.SubscribedApplicationRepository;
import com.example.tokenapijava.config.ApiKeyAuthenticationPrincipal;
import com.example.tokenapijava.scope.ApiKeyScopeRepository;
import com.example.tokenapijava.scope.ApiKeyScopeSchema;
import com.example.tokenapijava.utils.HashUtil;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyScopeRepository apiKeyScopeRepository;
    private final SubscribedApplicationRepository appsRpository;

    public ApiKeyFilter(ApiKeyRepository apiKeyRepository, ApiKeyScopeRepository apiKeyScopeRepository, SubscribedApplicationRepository repository) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyScopeRepository = apiKeyScopeRepository;
        this.appsRpository = repository;
    }

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{
        try{
            MDC.put("method","[" + request.getMethod() + "]");
            MDC.put("path", "[" + request.getRequestURI() + "]");

            String apiKeyValue = request.getHeader("X-Api-Key");
            if(apiKeyValue == null){
                filterChain.doFilter(request, response);
                return;
            }

            if(apiKeyRepository.findByHashedApiKey(HashUtil.sha256(apiKeyValue)).isEmpty()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            ApiKeySchema apiKey = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(apiKeyValue)).orElseThrow();
            AppsSchema app = null;
            switch(apiKey.getRoleType()){
                case CLASSIC:
                    MDC.put("apiKey","[" + apiKey.getHashedApiKey().substring(0, 12) + "...]");
                    if(apiKey.getStatus() == Status.REVOKED || apiKey.getStatus() == Status.EXPIRED || apiKey.getExpiresAt().isBefore(Instant.now().plusSeconds(20))){
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json");
                        response.setCharacterEncoding("UTF-8");
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String, Object> body = null;
                        if(apiKey.getStatus() == Status.REVOKED){
                            body = Map.of("error", "API_KEY_REVOKED", "message", "Your API Key has been revoked and is no longer usable.");
                        } else if (apiKey.getStatus() == Status.EXPIRED || apiKey.getExpiresAt().isBefore(Instant.now())){
                            body = Map.of("error", "API_KEY_EXPIRED", "message", "Your API Key has expired and is no longer usable. Please contact an administrator to recycle your API Key.");
                        }
                        objectMapper.writeValue(response.getWriter(), body);
                        return;
                    }
                    ApiKeyScopeSchema apiKeyScope = apiKeyScopeRepository.findById_HashedApiKey(apiKey.getHashedApiKey()).orElseThrow();
                    Long appId = apiKeyScope.getId().getAppId();
                    app = appsRpository.findById(appId).orElseThrow();
                    MDC.put("appId","(AppId: " + appId.toString() + ")");
                    break;
                case ADMIN:
                    MDC.put("apiKey","[ADMIN]");
                    String targetAppHeader = request.getHeader("X-Target-App"); 
                    if( targetAppHeader != null){
                        Long targetedAppId = Long.parseLong(targetAppHeader);
                        if(!appsRpository.existsById(targetedAppId)){
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            return;
                        }
                        app = appsRpository.findById(targetedAppId).orElseThrow();
                        MDC.put("appId","(for AppId: " + app.getId().toString() + ")");
                    } else {
                        app = null;
                    }
                    break;
                default:
                    break;
            }
            ApiKeyAuthenticationPrincipal principal = new ApiKeyAuthenticationPrincipal(apiKey, app);
            Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}