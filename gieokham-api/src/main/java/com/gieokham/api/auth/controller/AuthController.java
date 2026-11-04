package com.gieokham.api.auth.controller;

import com.gieokham.api.auth.dto.SignupRequest;
import com.gieokham.api.auth.security.CustomOidcUser;
import com.gieokham.api.member.domain.AuthProvider;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final MemberService memberService;
    private final SecurityContextRepository securityContextRepository;

    @GetMapping("/me")
    public Map<String, Object> getCurrentMember(@AuthenticationPrincipal CustomOidcUser principal) {
        Long memberId = principal.getMemberId();

        if (memberId == null) {
            String name = principal.getFullName();
            boolean nameProvided = name != null && !name.isBlank();

            return Map.of("registered", false,
                    "name", nameProvided ? name : "",
                    "nameEditable", !nameProvided
            );
        }

        return Map.of("registered", true, "memberId", memberId);
    }

    @GetMapping("/csrf")
    public Map<String, String> getCsrfToken(CsrfToken csrfToken) {
        return Map.of(
                "headerName", csrfToken.getHeaderName(),
                "token", csrfToken.getToken()
        );
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody SignupRequest signupRequest,
                                                      @AuthenticationPrincipal CustomOidcUser principal,
                                                      HttpServletRequest request, HttpServletResponse response) {

        Member member = memberService.registerMember(
                AuthProvider.GOOGLE,
                principal.getName(),
                signupRequest.getName(),
                signupRequest.getNickname(),
                principal.getEmail()
        );

        updateAuthentication(principal, member.getId(), request, response);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("registered", true, "memberId", member.getId()));
    }

    private void updateAuthentication(CustomOidcUser principal, Long memberId, HttpServletRequest request, HttpServletResponse response) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        for (GrantedAuthority authority : principal.getAuthorities()) {
            if (!"ROLE_PENDING".equals(authority.getAuthority())) {
                authorities.add(authority);
            }
        }

        authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));

        CustomOidcUser memberPrincipal = new CustomOidcUser(principal, authorities, memberId);

        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(memberPrincipal, authorities, "google");

        request.changeSessionId();

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }
}
