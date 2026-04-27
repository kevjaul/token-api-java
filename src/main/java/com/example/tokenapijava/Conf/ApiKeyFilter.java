package com.example.tokenapijava.Conf;

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

import com.example.tokenapijava.SubscribedApplicationRepository;
import com.example.tokenapijava.Schemas.AppsSchema;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {
    
    private final SubscribedApplicationRepository appsRpository;

    public ApiKeyFilter(SubscribedApplicationRepository repository) {
        this.appsRpository = repository;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith("/api/tokens/") && !request.getServletPath().startsWith("/api/apps/myApp");
    }
    @Override
    protected void doFilterInternal( HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException{
        String apiKey = request.getHeader("X-Api-Key");
        if (apiKey == null || appsRpository.findByHashedApiKey(HashUtil.sha256(apiKey)).isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        AppsSchema app = appsRpository.findByHashedApiKey(HashUtil.sha256(apiKey)).get();
        Authentication authentication = new UsernamePasswordAuthenticationToken(app, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}