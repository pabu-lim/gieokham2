package com.gieokham.api.config;

import com.gieokham.api.auth.security.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        http
                .securityContext(context ->
                    context.securityContextRepository(securityContextRepository)
                )
                .authorizeHttpRequests(auth ->
                auth.requestMatchers(
                        "/api/health",
                        "/error",
                        "/login/**",
                        "/oauth2/**"
                ).permitAll()
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/csrf"
                                ).authenticated()
                        .requestMatchers("/api/auth/signup").hasRole("PENDING")
                        .anyRequest().hasRole("MEMBER")
        ).oauth2Login(oauth2 ->
                oauth2.userInfoEndpoint(userInfo ->
                                userInfo.oidcUserService(customOidcUserService)
                        )
                        .defaultSuccessUrl("/api/auth/me", true)
        );

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
