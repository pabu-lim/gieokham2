package com.gieokham.api.config;

import com.gieokham.api.auth.security.CustomOidcUserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOidcUserService customOidcUserService;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository
    ) throws Exception {
        http
                .securityContext(context ->
                        context.securityContextRepository(
                                securityContextRepository
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/health",
                                "/error",
                                "/login/**",
                                "/oauth2/**"
                        ).permitAll()
                        .requestMatchers(
                                "/api/auth/me",
                                "/api/auth/csrf"
                        ).authenticated()
                        .requestMatchers("/api/auth/signup")
                        .hasRole("PENDING")
                        .anyRequest()
                        .hasRole("MEMBER")
                )
                .exceptionHandling(exception -> exception
                        // 인증되지 않은 요청
                        .authenticationEntryPoint((request, response, cause) ->
                                writeError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "로그인이 필요합니다."
                                )
                        )
                        // 권한 부족 또는 CSRF 검증 실패
                        .accessDeniedHandler((request, response, cause) -> {
                            Authentication authentication =
                                    SecurityContextHolder.getContext()
                                            .getAuthentication();

                            boolean authenticated =
                                    authentication != null
                                            && authentication.isAuthenticated()
                                            && !(authentication
                                            instanceof AnonymousAuthenticationToken);

                            if (!authenticated) {
                                writeError(
                                        response,
                                        HttpServletResponse.SC_UNAUTHORIZED,
                                        "로그인이 필요합니다."
                                );
                                return;
                            }

                            String message = cause instanceof CsrfException
                                    ? "요청 인증 정보가 유효하지 않습니다. 다시 시도해 주세요."
                                    : "접근 권한이 없습니다.";

                            writeError(
                                    response,
                                    HttpServletResponse.SC_FORBIDDEN,
                                    message
                            );
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo ->
                                userInfo.oidcUserService(customOidcUserService)
                        )
                        .defaultSuccessUrl("/api/auth/me", true)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(
                                        HttpServletResponse.SC_NO_CONTENT
                                )
                        )
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    private void writeError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"message\":\"" + message + "\"}"
        );
    }
}