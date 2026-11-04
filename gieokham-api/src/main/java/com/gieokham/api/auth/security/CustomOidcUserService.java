package com.gieokham.api.auth.security;

import com.gieokham.api.member.domain.AuthProvider;
import com.gieokham.api.member.domain.Member;
import com.gieokham.api.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final MemberService memberService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) {
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        if (!"google".equals(registrationId)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"),
                    "지원하지 않는 로그인 방식입니다.");
        }

        OidcUser oidcUser = super.loadUser(userRequest);

        Optional<Member> member = memberService.findMember(AuthProvider.GOOGLE, oidcUser.getName());

        List<GrantedAuthority> authorities = new ArrayList<>(oidcUser.getAuthorities());

        Long memberId = null;

        if (member.isPresent()) {
            memberId = member.get().getId();
            authorities.add(new SimpleGrantedAuthority("ROLE_MEMBER"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_PENDING"));
        }

        return new CustomOidcUser(oidcUser, authorities, memberId);
    }
}
