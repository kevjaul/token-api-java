package com.example.tokenapijava.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springdoc.core.customizers.OpenApiCustomizer;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.example.tokenapijava.apiKey.ApiKeyFilter;

@Configuration
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;

    private final RateLimitFilter rateLimitFilter;

    public SecurityConfig(ApiKeyFilter apiKeyFilter, RateLimitFilter rateLimitFilter) {
        this.apiKeyFilter = apiKeyFilter;
        this.rateLimitFilter = rateLimitFilter;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes("apiKeyAuth", 
                    new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-Api-Key"))
            )

            .info(new Info()
                .title("Token API")
                .version("1.2.5")
                .description("""
                    API de gestion de tokens. (DEMONSTRATION)

                    Rate Limiting global :
                        - 20 requêtes GET / POST / PUT par jour
                        - 5 requêtes DELETE par jour
                        - Stratégie de limitation par Token Bucket avec fenêtre glissante (1j)
                        - Limitation par API Key
                        - API Key valable 30 jours (avant recyclage)
                """)
            );
    }

    @Bean
    public OpenApiCustomizer globalHeaderCustomizer() {
        return openApi -> openApi.getPaths().forEach((path, pathItem) -> {

            pathItem.readOperations().forEach(operation -> {

                boolean hasApiKeySecurity = operation.getSecurity() != null
                    && operation.getSecurity().stream()
                        .anyMatch(sec -> sec.containsKey("apiKeyAuth"));

                if (hasApiKeySecurity) {
                    operation.addParametersItem(
                        new Parameter()
                            .in("header")
                            .name("X-Target-App")
                            .required(false)
                            .schema(new StringSchema())
                            .description("Only required if an ADMIN key is used")
                    );
                }
            });
        });
    }

    @Bean
    @Order(1)
    SecurityFilterChain apiKeyFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher( "/api/**")
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(request -> request
                .requestMatchers("/api/apps/register").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, ApiKeyFilter.class);
        return http.build();
    }
    @Bean
    @Order(2)
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/apps/**").permitAll()
                        .requestMatchers("/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**").permitAll()
                        .anyRequest().permitAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService testOnlyUsers(PasswordEncoder passwordEncoder) {
        User.UserBuilder users = User.builder();
        UserDetails user1 = users
                .username("userTest1")
                .password(passwordEncoder.encode("aaa111"))
                .build();
        UserDetails user2 = users
                .username("userTest2")
                .password(passwordEncoder.encode("bbb222"))
                .build();
        return new InMemoryUserDetailsManager(user1, user2);
    }
}
