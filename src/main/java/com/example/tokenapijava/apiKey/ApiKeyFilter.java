package com.example.tokenapijava.apiKey;

import java.io.IOException;
import java.util.List;

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
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/tokens/") && !request.getServletPath().startsWith("/api/apps/myApp");
    }

    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{
        String apiKeyValue = request.getHeader("X-Api-Key");
       
        if (apiKeyValue == null || apiKeyRepository.findByHashedApiKey(HashUtil.sha256(apiKeyValue)).isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        ApiKeySchema apiKey = apiKeyRepository.findByHashedApiKey(HashUtil.sha256(apiKeyValue)).orElseThrow();
        AppsSchema app = null;
        switch(apiKey.getRoleType()){
            case CLASSIC:
                ApiKeyScopeSchema apiKeyScope = apiKeyScopeRepository.findById_HashedApiKey(apiKey.getHashedApiKey());
                Long appId = apiKeyScope.getId().getAppId();
                app = appsRpository.findById(appId).orElseThrow();
                break;
            case ADMIN:
                if(request.getHeader("X-Target-App").isEmpty() || !appsRpository.existsById(Long.parseLong(request.getHeader("X-Target-App")))){
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
                app = appsRpository.findById(Long.parseLong(request.getHeader("X-Target-App"))).orElseThrow();
                break;
            default:
                break;
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(app, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}